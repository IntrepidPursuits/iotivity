package org.iotivity.ca.service;

/**
 *
 * Created by Anton Spaans on 5/29/15.
 */
public interface RMInterfaceIfc {
    void RMStartListeningServer();

    void RMStartDiscoveryServer();

    void RMFindResource(String uri);

    void RMSendRequest(String uri, String payload,
                       int selectedNetwork, int isSecured, int msgType);

    void RMSendReqestToAll(String uri, int selectedNetwork);

    void RMSendResponse(int selectedNetwork, int isSecured,
                        int msgType, int responseValue);

    void RMAdvertiseResource(String advertiseResource);

    void RMSendNotification(String uri, String payload,
                            int selectedNetwork, int isSecured, int msgType, int responseValue);

    void RMSelectNetwork(int interestedNetwork);

    void RMUnSelectNetwork(int uninterestedNetwork);

    void RMGetNetworkInfomation();

    void RMHandleRequestResponse();

    void sendRequest(String uri, String payload, int selectedNetwork, int method, int msgType);
}
