package org.iotivity.ca.service;

import android.content.Context;

/**
 *
 * Implements the native RMInterface.
 */
class RMInterface implements RMInterfaceIfc {

    static {
        // Load RI JNI interface
        System.loadLibrary("RMInterface");
    }

    RMInterface(Context context) {
        RMInitialize(context);
        RMRegisterHandler();
        setNativeResponseListener(this);
    }

    void close() {
        setNativeResponseListener(null);
        RMTerminate();
    }

    private native void setNativeResponseListener(Object listener);

    private native void RMInitialize(Context context);

    private native void RMRegisterHandler();

    private native void RMTerminate();

    @Override
    public native void RMStartListeningServer();

    @Override
    public native void RMStartDiscoveryServer();

    @Override
    public native void RMFindResource(String uri);

    @Override
    public native void RMSendRequest(String uri, String payload,
                                     int selectedNetwork, int isSecured, int msgType);

    @Override
    public native void RMSendReqestToAll(String uri, int selectedNetwork);

    @Override
    public native void RMSendResponse(int selectedNetwork, int isSecured,
                                      int msgType, int responseValue);

    @Override
    public native void RMAdvertiseResource(String advertiseResource);

    @Override
    public native void RMSendNotification(String uri, String payload,
                                          int selectedNetwork, int isSecured, int msgType, int responseValue);

    @Override
    public native void RMSelectNetwork(int interestedNetwork);

    @Override
    public native void RMUnSelectNetwork(int uninterestedNetwork);

    @Override
    public native void RMGetNetworkInfomation();

    @Override
    public native void RMHandleRequestResponse();

    @Override
    public native void sendRequest(String uri, String payload, int selectedNetwork, int method, int msgType);

    @SuppressWarnings("unused")
    private void OnResponseReceived(String subject, String receivedData) {
        RMInterfaceFactory.notifyReponseListener(subject, receivedData);
    }
}
