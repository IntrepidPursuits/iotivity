
#ifdef __ANDROID__
#include <android/log.h>
#endif

#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "logger.h"
#include "oic_malloc.h"

#include "cainterface.h"
#include "cacommon.h"

#include "../include/MeshCop.h"

#define TAG "MeshCop"

#define STRLEN(STR) ( (STR)? strlen(STR) : 0 )
#define STRCPY(STR) ( (STR)? strcpy(calloc(STRLEN(STR) + 1, sizeof(char)), STR) : NULL )

#define NEW_TLV_COMMISSIONER_ID(ID) ( newTLV(TLV_COMMISSIONER_ID, STRLEN(ID), (MC_Value){ .rawVal = (ID)}) )
#define NEW_TLV_COMMISSIONER_SESSION_ID(ID) ( newTLV(TLV_COMMISSIONER_SESSION_ID, STRLEN(ID), (MC_Value){ .rawVal = (ID)}) )
#define NEW_TLV_STATE(STATE) ( newTLV(TLV_STATE, sizeof(uint8_t), (MC_Value){ .byteVal = (STATE)}) )

// Header info
typedef union {
    uint8_t  byteVal;
    uint16_t shortVal;
    uint32_t intVal;
    long     longVal;
    char*    rawVal;
} MC_Value;

typedef struct {
    uint8_t  type;
    uint8_t  length;
    MC_Value value;
} MCTLV_t;

MCTLV_t* newTLV(uint8_t type, uint8_t length, MC_Value mcValue);
void destroyTLVs(MCTLV_t *tlvs, uint8_t numTlvs);
void destroyTLVReferences(MCTLV_t **tlvRefs, uint8_t numTlvs);

void determineTLVBufferLength(MCTLV_t *tlv, uint32_t *bufferStart);
MCTLV_t* writeBufferToTLV(char* buffer, uint32_t *bufferStart);
void writeTLVToBuffer(MCTLV_t *tlv, char* buffer, uint32_t *bufferStart);
CAResult_t writeTLVsToRequestData(MCTLV_t **tlvs, uint8_t numTlvs, CAInfo_t *requestData);

void logTLV(MCTLV_t *tlv);

uint32_t get_uri(char *port, char *path, char **strUriOut);
CAResult_t get_network_type2(uint32_t selectedNetwork, CATransportType_t* networkType);
char* get_remote_address2(CATransportType_t transportType, CAAddress_t addressInfo);

uint8_t  g_selectedNetwork;
uint8_t  g_isSecure = 0;
char*    g_BRHostName = NULL;

char* g_portCOAP  = ":5683";
char* g_portCOAPS = ":5684";
char* g_portMC    = ":19779";
char* g_portMJ    = ":19786";
char* g_portMM    = ":19786"; // ????

#define COAP_SCHEME "coap://"
#define COAPS_SCHEME "coaps://"

CAResult_t prologue(char* port, char* path, CARemoteEndpoint_t **endpointOut, CAInfo_t *requestDataInOut) {
    assert(requestDataInOut != NULL);

    char* strUri = NULL;
    uint32_t length = get_uri(port, path, &strUri);
    if (length < 0) {
        OICLog(ERROR, TAG, "Could not generate a URI.");
        return CA_MEMORY_ALLOC_FAILED;
    }

    // Determine network type
    CATransportType_t networkType;
    CAResult_t res = get_network_type2(g_selectedNetwork, &networkType);
    if (CA_STATUS_OK != res) {
        OICLog(ERROR, TAG, "Could not select appropriate network type.");

        OICFree(strUri);
        return CA_STATUS_INVALID_PARAM;
    }

    // Create remote endpoint
    *endpointOut = NULL;
    res = CACreateRemoteEndpoint((const CAURI_t)strUri, networkType, endpointOut);
    if (CA_STATUS_OK != res) {
        OICLog(ERROR, TAG, "Could not create remote end point");

        OICFree(strUri);
        return CA_MEMORY_ALLOC_FAILED;
    }

    OICFree(strUri);

    // Create options/headers
    uint32_t optionNum = 2;
    CAHeaderOption_t *headerOpt = (CAHeaderOption_t*) calloc(1, sizeof(CAHeaderOption_t) * optionNum);
    if (NULL == headerOpt) {
        OICLog(ERROR, TAG, "Memory allocation for options failed");

        CADestroyRemoteEndpoint(*endpointOut); *endpointOut = NULL;
        return CA_MEMORY_ALLOC_FAILED;
    }

    const uint16_t ctApplicationOctetStream = 42;

    headerOpt[0].optionID = 12; // Content-Type
    headerOpt[0].optionLength = sizeof(uint16_t);
    memcpy(headerOpt[0].optionData, &ctApplicationOctetStream, sizeof(uint16_t));

    headerOpt[1].optionID = 17; // Accept
    headerOpt[1].optionLength = sizeof(uint16_t);
    memcpy(headerOpt[1].optionData, &ctApplicationOctetStream, sizeof(uint16_t));

    // Create token
    CAToken_t token = NULL;
    uint8_t tokenLength = CA_MAX_TOKEN_LEN;

    res = CAGenerateToken(&token, tokenLength);
    if ((CA_STATUS_OK != res) || (!token))
    {
        OICLog(ERROR, TAG, "token generate error");

        OICFree(headerOpt);
        CADestroyRemoteEndpoint(*endpointOut); *endpointOut = NULL;
        return res == CA_STATUS_OK? CA_MEMORY_ALLOC_FAILED : res;
    }

    requestDataInOut->options = headerOpt;
    requestDataInOut->numOptions = optionNum;
    requestDataInOut->token = token;
    requestDataInOut->tokenLength = tokenLength;

    return CA_STATUS_OK;
}

void epilogue(CARemoteEndpoint_t *endpoint, CAInfo_t *requestData) {
    if (endpoint) {
        OICFree(endpoint);
    }

    if (requestData) {
        if (requestData->payload) {
            OICFree(requestData->payload);
        }
        if (requestData->options) {
            OICFree(requestData->options);
        }
    }
}


CAToken_t COMM_PET_request(char* commissionerId) {
    CARemoteEndpoint_t* endpoint = NULL;

    CAInfo_t requestData = { 0 };
    requestData.type = CA_MSG_CONFIRM;

    CARequestInfo_t requestInfo = { 0 };
    requestInfo.method = CA_POST;
    requestInfo.info = requestData;

    CAResult_t res = prologue(g_portMC, "/tn/mc/cp", &endpoint, &requestData);
    if (res != CA_STATUS_OK) {
        return NULL;
    }

    MCTLV_t *tlvCommissionerId = NEW_TLV_COMMISSIONER_ID(commissionerId);
    if (!tlvCommissionerId) {
        OICLog(ERROR, TAG, "TLV generate error");

        epilogue(endpoint, &requestData);
        return NULL;
    }

    res = writeTLVsToRequestData(&tlvCommissionerId, 1, &requestData);
    if (res != CA_STATUS_OK) {
        OICLog(ERROR, TAG, "TLV payload generation error");

        destroyTLVs(tlvCommissionerId, 1);
        epilogue(endpoint, &requestData);
        return NULL;
    }
    destroyTLVs(tlvCommissionerId, 1);

    // Send request
    char* token = NULL;
    if (CA_STATUS_OK != CASendRequest(endpoint, &requestInfo)) {
        OICLog(ERROR, TAG, "Could not send request");

        OICFree(requestData.token);
        requestData.token = NULL;
    }
    else {
        OICLog(INFO, TAG, "Request sent successfully");

        token = requestData.token;
    }

    // Clean up.
    epilogue(endpoint, &requestData);

    return token;
}

CAToken_t COMM_KA_request(MCState_t state, char* commissionerSessionId) {
    CARemoteEndpoint_t* endpoint = NULL;

    CAInfo_t requestData = { 0 };
    requestData.type = CA_MSG_CONFIRM;

    CARequestInfo_t requestInfo = { 0 };
    requestInfo.method = CA_POST;
    requestInfo.info = requestData;

    CAResult_t res = prologue(g_portMC, "/tn/mc/ca", &endpoint, &requestData);
    if (res != CA_STATUS_OK) {
        return NULL;
    }

    MCTLV_t** tlvs = calloc(2, sizeof(MCTLV_t*));
    tlvs[0] = NEW_TLV_STATE(state);
    if (!tlvs[0]) {
        OICLog(ERROR, TAG, "TLV generate error: State");

        destroyTLVReferences(tlvs, 2);
        epilogue(endpoint, &requestData);
        return NULL;
    }

    tlvs[1] = NEW_TLV_COMMISSIONER_ID(commissionerSessionId);
    if (!tlvs[1]) {
        OICLog(ERROR, TAG, "TLV generate error: CommissionerSessionId");

        destroyTLVReferences(tlvs, 2);
        epilogue(endpoint, &requestData);
        return NULL;
    }

    res = writeTLVsToRequestData(tlvs, 2, &requestData);
    if (res != CA_STATUS_OK) {
        OICLog(ERROR, TAG, "TLV payload generation error");

        destroyTLVReferences(tlvs, 2);
        epilogue(endpoint, &requestData);
        return NULL;
    }
    destroyTLVReferences(tlvs, 2);

    // Send request
    char* token = NULL;
    if (CA_STATUS_OK != CASendRequest(endpoint, &requestInfo)) {
        OICLog(ERROR, TAG, "Could not send request");

        OICFree(requestData.token);
        requestData.token = NULL;
    }
    else {
        OICLog(INFO, TAG, "Request sent successfully");

        token = requestData.token;
    }

    // Clean up.
    epilogue(endpoint, &requestData);

    return token;
}

uint8_t isRawType(uint8_t type) {
    switch (type) {
    case TLV_COMMISSIONER_ID:
    case TLV_COMMISSIONER_SESSION_ID:
        return true;
    default:
        return false;
    }
}

MCTLV_t* newTLV(uint8_t type, uint8_t length, MC_Value mcValue) {
    assert(length > 0);

    uint8_t isRaw = isRawType(type);

    MCTLV_t *retVal = calloc(1, sizeof(MCTLV_t) + (isRaw? length + 1 : 0));
    if (!retVal) {
        return NULL;
    }
    retVal->type = type;
    retVal->length = length;

    if (isRaw) {
        retVal->value.rawVal = (char*)retVal + sizeof(MCTLV_t);
        if (mcValue.rawVal) {
            memcpy(retVal->value.rawVal, mcValue.rawVal, length);
        }
        retVal->value.rawVal[length] = 0; // in case raw value is a string.
    }
    else {
        retVal->value = mcValue;
    }

    return retVal;
}

void destroyTLVs(MCTLV_t *tlvs, uint8_t numTlvs) {
    if (!tlvs) {
        return;
    }
    uint8_t i;
    for (i = 0; i < numTlvs; i++) {
        free(&tlvs[i]);
    }
}

void destroyTLVReferences(MCTLV_t **tlvRefs, uint8_t numTlvs) {
    if (!tlvRefs) {
        return;
    }

    uint8_t i;
    for (i = 0; i < numTlvs; i++) {
        if (tlvRefs[i]) {
            free(tlvRefs[i]);
        }
    }
}

void determineTLVBufferLength(MCTLV_t *tlv, uint32_t *bufferStart) {
    assert(tlv != NULL);
    assert(bufferStart != NULL);
    *bufferStart += (2 + tlv->length);
}

void writeTLVToBuffer(MCTLV_t *tlv, char* buffer, uint32_t *bufferStart) {
    assert(tlv != NULL);
    assert(buffer != NULL);

    uint32_t start = bufferStart? *bufferStart : 0;
    buffer[start++] = tlv->type;
    buffer[start++] = tlv->length;

    if (isRawType(tlv->type)) {
        memcpy(buffer + start, tlv->value.rawVal, tlv->length);
    }
    else {
        memcpy(buffer + start, &(tlv->value), tlv->length);
    }

    if (bufferStart) {
        *bufferStart += (2 + tlv->length);
    }
}

MCTLV_t* writeBufferToTLV(char* buffer, uint32_t *bufferStart) {
    assert(buffer != NULL);

    uint32_t start = bufferStart? *bufferStart : 0;
    uint8_t type = buffer[start++];
    uint8_t length = buffer[start++];

    MCTLV_t *tlv = newTLV(type, length, (MC_Value){ 0 });
    if (!tlv) {
        return NULL;
    }
    uint8_t isRaw = isRawType(type);

    if (isRaw) {
        memcpy(tlv->value.rawVal, buffer + start, length);
        tlv->value.rawVal[length] = 0; // in case raw value is a string.
    }
    else {
        memcpy(&(tlv->value), buffer + start, length);
    }

    if (bufferStart) {
        *bufferStart += (2 + length);
    }

    return tlv;
}

void logTLV(MCTLV_t *tlv) {
    if (isRawType(tlv->type)) {
        OICLogv(INFO, TAG, "TLV[type = %d; length = %d, value='%s']", tlv->type, tlv->length, tlv->value.rawVal);
    }
    else {
        OICLogv(INFO, TAG, "TLV[type = %d; length = %d, value=%d]", tlv->type, tlv->length, (uint32_t)tlv->value.longVal);
    }
}

CAResult_t writeTLVsToRequestData(MCTLV_t **tlvs, uint8_t numTlvs, CAInfo_t *requestData) {
    uint32_t bufferLen = 0;
    uint8_t i;
    for (i = 0; i < numTlvs; i++) {
        determineTLVBufferLength(tlvs[i], &bufferLen);
    }
    char *buffer = malloc(bufferLen + 1);
    if (!buffer) {
        OICLog(ERROR, TAG, "TLV Buffer generate error");
        return CA_MEMORY_ALLOC_FAILED;
    }

    uint32_t start = 0;
    for (i = 0; i < numTlvs; i++) {
        writeTLVToBuffer(tlvs[i], buffer, &start);
    }
    buffer[bufferLen] = 0;

    requestData->payload = buffer;
    requestData->payloadLength = bufferLen;

    return CA_STATUS_OK;
}

uint32_t get_uri(char *port, char *path, char **strUriOut) {
    assert(strUriOut != NULL);

    uint32_t length = strlen(g_isSecure? COAPS_SCHEME : COAP_SCHEME) + strlen(g_BRHostName) + strlen(port) + strlen(path);
    *strUriOut = malloc(length + 1);
    if (*strUriOut == NULL) {
        return -1;
    }
    return snprintf(*strUriOut, length, "%s%s%s%s", g_isSecure? COAPS_SCHEME : COAP_SCHEME, g_BRHostName, port, path);
}

CAResult_t get_network_type2(uint32_t selectedNetwork, CATransportType_t* networkType)
{
    uint32_t number = selectedNetwork;
    if (!(number & 0xf))
    {
        return CA_NOT_SUPPORTED;
    }
    if (number & CA_IPV4)
    {
        *networkType = CA_IPV4;
        return CA_STATUS_OK;
    }
    if (number & CA_IPV6)
    {
        *networkType = CA_IPV6;
        return CA_STATUS_OK;
    }
    if (number & CA_EDR)
    {
        *networkType = CA_EDR;
        return CA_STATUS_OK;
    }
    if (number & CA_LE)
    {
        *networkType = CA_LE;
        return CA_STATUS_OK;
    }

    return CA_NOT_SUPPORTED;
}

char* get_remote_address2(CATransportType_t transportType, CAAddress_t addressInfo)
{
    char* remoteAddress = NULL;
    uint32_t len = 0;
    if (CA_IPV4 == transportType)
    {
        len = strlen(addressInfo.IP.ipAddress);
        remoteAddress = (char *) malloc(sizeof(char) * (len + 1));

        if (NULL == remoteAddress)
        {
            OICLog(ERROR, TAG, "remoteAddress Out of memory");
            return NULL;
        }

        memcpy(remoteAddress, addressInfo.IP.ipAddress, len + 1);
    }

    else if (CA_EDR == transportType)
    {
        len = strlen(addressInfo.BT.btMacAddress);
        remoteAddress = (char *) malloc(sizeof(char) * (len + 1));

        if (NULL == remoteAddress)
        {
            OICLog(ERROR, TAG, "remoteAddress Out of memory");
            return NULL;
        }

        memcpy(remoteAddress, addressInfo.BT.btMacAddress, len + 1);
    }

    else if (CA_LE == transportType)
    {
        len = strlen(addressInfo.LE.leMacAddress);
        remoteAddress = (char *) malloc(sizeof(char) * (len + 1));

        if (NULL == remoteAddress)
        {
            OICLog(ERROR, TAG, "remoteAddress Out of memory");
            return NULL;
        }

        memcpy(remoteAddress, addressInfo.LE.leMacAddress, len + 1);
    }

    return remoteAddress;
}
