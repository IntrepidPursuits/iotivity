/******************************************************************
 *
 * Copyright 2014 Samsung Electronics All Rights Reserved.
 *
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>

#include "cainterface.h"
#include "caremotehandler.h"
#include "caprotocolmessage.h"
#include "canetworkconfigurator.h"
#include "logger.h"

#include "cainterfacecontroller_singlethread.h"
#include "camessagehandler_singlethread.h"

#define TAG "CM_ST"

static bool g_isInitialized = false;

CAResult_t CAInitialize()
{
    OIC_LOG(DEBUG, TAG, "IN");

    if (!g_isInitialized)
    {
        CAResult_t res = CAInitializeMessageHandler();
        if (res != CA_STATUS_OK)
        {
            OIC_LOG(ERROR, TAG, "not initialized");
            return res;
        }
        g_isInitialized = true;
    }

    return CA_STATUS_OK;
}

void CATerminate()
{
    OIC_LOG(DEBUG, TAG, "IN");

    if (g_isInitialized)
    {
        CASetInterfaceCallbacks(NULL, NULL, NULL);
        CATerminateMessageHandler();
        CATerminateNetworkType();
        g_isInitialized = false;
    }

    OIC_LOG(DEBUG, TAG, "OUT");
}

CAResult_t CAStartListeningServer()
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CAStartListeningServerAdapters();
}

CAResult_t CAStartDiscoveryServer()
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CAStartDiscoveryServerAdapters();
}

void CARegisterHandler(CARequestCallback ReqHandler, CAResponseCallback RespHandler,
                       CAErrorCallback errorHandler)
{
    OIC_LOG(DEBUG, TAG, "IN");

    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return;
    }

    CASetInterfaceCallbacks(ReqHandler, RespHandler, errorHandler);
    OIC_LOG(DEBUG, TAG, "OUT");
}

CAResult_t CACreateRemoteEndpoint(const CAURI_t uri, const CATransportType_t transportType,
                                  CARemoteEndpoint_t **remoteEndpoint)
{
    OIC_LOG(DEBUG, TAG, "IN");

    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    CARemoteEndpoint_t *remote = CACreateRemoteEndpointUriInternal(uri, transportType);

    if (remote == NULL)
    {
        OIC_LOG(ERROR, TAG, "remote endpoint is NULL");
        return CA_STATUS_FAILED;
    }

    *remoteEndpoint = remote;

    OIC_LOG(DEBUG, TAG, "OUT");
    return CA_STATUS_OK;
}

void CADestroyRemoteEndpoint(CARemoteEndpoint_t *rep)
{
    CADestroyRemoteEndpointInternal(rep);
}

CAResult_t CAGenerateToken(CAToken_t *token, uint8_t tokenLength)
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CAGenerateTokenInternal(token, tokenLength);
}

void CADestroyToken(CAToken_t token)
{
    OIC_LOG(DEBUG, TAG, "IN");

    CADestroyTokenInternal(token);
    OIC_LOG(DEBUG, TAG, "OUT");
}

CAResult_t CAGetNetworkInformation(CALocalConnectivity_t **info, uint32_t *size)
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CAGetNetworkInformationInternal(info, size);
}

CAResult_t CAFindResource(const CAURI_t resourceUri, const CAToken_t token, uint8_t tokenLength)
{
    OIC_LOG(DEBUG, TAG, "IN");

    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CADetachMessageResourceUri(resourceUri, token, tokenLength, NULL, 0);
}

CAResult_t CASendRequest(const CARemoteEndpoint_t *object,const CARequestInfo_t *requestInfo)
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CADetachRequestMessage(object, requestInfo);
}

CAResult_t CASendRequestToAll(const CAGroupEndpoint_t *object,
                              const CARequestInfo_t *requestInfo)
{
    OIC_LOG(DEBUG, TAG, "CASendRequestToAll");

    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CADetachRequestToAllMessage(object, requestInfo);
}

CAResult_t CASendNotification(const CARemoteEndpoint_t *object,
    const CAResponseInfo_t *responseInfo)
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CADetachResponseMessage(object, responseInfo);
}

CAResult_t CASendResponse(const CARemoteEndpoint_t *object,
    const CAResponseInfo_t *responseInfo)
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    return CADetachResponseMessage(object, responseInfo);
}

CAResult_t CAAdvertiseResource(const CAURI_t resourceUri,const CAToken_t token,
                               uint8_t tokenLength, const CAHeaderOption_t *options,
                               const uint8_t numOptions)
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }
    return CADetachMessageResourceUri(resourceUri, token, tokenLength, options, numOptions);
}

CAResult_t CASelectNetwork(const uint32_t interestedNetwork)
{
    OIC_LOG_V(DEBUG, TAG, "Selected n/W=%d", interestedNetwork);

    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    if (!(interestedNetwork & 0xf))
    {
        OIC_LOG(ERROR, TAG, "not supported");
        return CA_NOT_SUPPORTED;
    }
    CAResult_t res = CA_STATUS_OK;

    if (interestedNetwork & CA_IPV4)
    {
        res = CAAddNetworkType(CA_IPV4);
        if (res != CA_STATUS_OK)
        {
            OIC_LOG(ERROR, TAG, "Failed to Add n/w type");
            return res;
        }
    }

    if (interestedNetwork & CA_EDR)
    {
        res = CAAddNetworkType(CA_EDR);
        if (res != CA_STATUS_OK)
        {
            OIC_LOG(ERROR, TAG, "Failed to Add n/w type");
            return res;
        }
    }

    if (interestedNetwork & CA_LE)
    {
        res = CAAddNetworkType(CA_LE);
        if (res != CA_STATUS_OK)
        {
            OIC_LOG(ERROR, TAG, "Failed to Add n/w type");
            return res;
        }
    }
    OIC_LOG(DEBUG, TAG, "OUT");
    return res;
}

CAResult_t CAUnSelectNetwork(const uint32_t nonInterestedNetwork)
{
    OIC_LOG_V(DEBUG, TAG, "unselected n/w=%d", nonInterestedNetwork);

    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    if (!(nonInterestedNetwork & 0xf))
    {
        OIC_LOG(ERROR, TAG, "not supported");
        return CA_NOT_SUPPORTED;
    }

    CAResult_t res = CA_STATUS_OK;

    if (nonInterestedNetwork & CA_IPV4)
    {
        res = CARemoveNetworkType(CA_IPV4);
        if (res != CA_STATUS_OK)
        {
            OIC_LOG(ERROR, TAG, "Failed to remove n/w type");
            return res;
        }
    }

    if (nonInterestedNetwork & CA_EDR)
    {
        res = CARemoveNetworkType(CA_EDR);
        if (res != CA_STATUS_OK)
        {
            OIC_LOG(ERROR, TAG, "Failed to remove n/w type");
            return res;
        }
    }

    if (nonInterestedNetwork & CA_LE)
    {
        res = CARemoveNetworkType(CA_LE);
        if (res != CA_STATUS_OK)
        {
            OIC_LOG(ERROR, TAG, "Failed to remove n/w type");
            return res;
        }
    }
    OIC_LOG(DEBUG, TAG, "OUT");
    return res;
}

CAResult_t CAHandleRequestResponse()
{
    if (!g_isInitialized)
    {
        OIC_LOG(ERROR, TAG, "not initialized");
        return CA_STATUS_NOT_INITIALIZED;
    }

    CAHandleRequestResponseCallbacks();
    return CA_STATUS_OK;
}

