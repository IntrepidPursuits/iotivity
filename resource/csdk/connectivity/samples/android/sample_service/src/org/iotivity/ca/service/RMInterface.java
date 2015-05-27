package org.iotivity.ca.service;

import android.content.Context;
import android.os.Build;

public class RMInterface {

    static {
        // Load RI JNI interface
    	int x = Build.VERSION.SDK_INT;
    	int y = Build.VERSION_CODES.BASE;
        System.loadLibrary("RMInterface");
    }

    private org.iotivity.ca.service.MainActivity mResponseListener = null;

    public native void setNativeResponseListener(Object listener);

    public native void RMInitialize(Context context);

    public native void RMTerminate();

    public native void RMStartListeningServer();

    public native void RMStartDiscoveryServer();

    public native void RMRegisterHandler();

    public native void RMFindResource(String uri);

    public native void RMSendRequest(String uri, String payload,
            int selectedNetwork, int isSecured, int msgType);

    public native void RMSendReqestToAll(String uri, int selectedNetwork);

    public native void RMSendResponse(int selectedNetwork, int isSecured,
            int msgType, int responseValue);

    public native void RMAdvertiseResource(String advertiseResource);

    public native void RMSendNotification(String uri, String payload,
            int selectedNetwork, int isSecured, int msgType, int responseValue);

    public native void RMSelectNetwork(int interestedNetwork);

    public native void RMUnSelectNetwork(int uninterestedNetwork);

    public native void RMGetNetworkInfomation();

    public native void RMHandleRequestResponse();

    public void setResponseListener(org.iotivity.ca.service.MainActivity listener) {
        mResponseListener = listener;
        setNativeResponseListener(this);
    }

    private void OnResponseReceived(String subject, String receivedData) {
        if (null != mResponseListener) {
            mResponseListener.OnResponseReceived(subject, receivedData);
        }
    }

}