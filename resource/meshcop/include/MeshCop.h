
#ifndef MESH_COP_H_
#define MESH_COP_H_

#include "cacommon.h"


typedef enum {
    REJECT = -1,
    PENDING = 0,
    ACCEPT = 1
} MCState_t;

#define TLV_COMMISSIONER_ID 10
#define TLV_COMMISSIONER_SESSION_ID 11
#define TLV_STATE 16

CAToken_t COMM_PET_request(char* commissionerId);
CAToken_t COMM_KA_request(MCState_t state, char* commissionerSessionId);

#endif /* MESH_COP_H_ */


