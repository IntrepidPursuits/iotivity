package org.threadgroup.iotivity.activities;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import android.util.Log;

import org.iotivity.ca.service.RMInterfaceFactory;
import org.iotivity.ca.service.RMInterfaceIfc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by aspaans on 6/2/15.
 */
class DnsService {
    // Create the NsdServiceInfo object, and populate it.
    static final NsdServiceInfo SERVICE_INFO = new NsdServiceInfo();

    // The name is subject to change based on conflicts
    // with other services advertised on the same network.
    static {
        SERVICE_INFO.setServiceName("ThreadGroupNetAndroid1");
        SERVICE_INFO.setServiceType("_thread-net._udp");
        SERVICE_INFO.setPort(8123);
    }

    Context appContext;
    NsdManager mNsdManager;
    DnsResolver resolver;

    private NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {

        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {

        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            if (TextUtils.isEmpty(serviceInfo.getServiceType())) {
                serviceInfo.setServiceType(SERVICE_INFO.getServiceType());
            }
            resolver.resolve(serviceInfo);
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            if (datagramReceiver != null) {
                datagramReceiver.kill();
            }
        }
    };

    NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e("DnsService", "Resolved Failed " + serviceInfo.toString() + "; error: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e("DnsService", "Resolve succeeded " + serviceInfo.toString());

//            datagramReceiver = new MyDatagramReceiver(appContext, serviceInfo.getHost(), serviceInfo.getPort());
//            datagramReceiver.start();
        }
    };

    MyDatagramReceiver datagramReceiver;

    public DnsService() {
    }

    void register(Context context) {
        appContext = context.getApplicationContext();
        if (resolver == null) {
            resolver = DnsResolver.get(context);
        }
        resolver.addListener(resolveListener);
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(SERVICE_INFO, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    void unregister() {
        mNsdManager.unregisterService(mRegistrationListener);
        resolver.removeListener(resolveListener);
    }

    private static class MyDatagramReceiver extends Thread {
        private static int CA_IPV4 = (1 << 0);
        private static int CA_IPV6 = (1 << 1);
        private static int CA_EDR = (1 << 2);
        private static int CA_LE = (1 << 3);

        private static int MSG_TYPE_CON = 0;
        private static int MSG_TYPE_NON = 1;
        private static int MSG_TYPE_ACK = 2;
        private static int MSG_TYPE_RST = 3;

        private static int METHOD_GET = 1;
        private static int METHOD_POST = 2;
        private static int METHOD_PUT = 3;
        private static int METHOD_DELETE = 4;

        private static int MAX_UDP_DATAGRAM_LEN = 1500;

        private Context context;
        private boolean bKeepRunning = true;
        private String lastMessage = "";
        private final InetAddress address;
        private final int port;

        private DatagramSocket socket;

        private RMInterfaceIfc rmInterface;

        MyDatagramReceiver(Context context, InetAddress address, int port) {
            this.address = address;
            this.port = port;

            rmInterface = RMInterfaceFactory.obtain(context);
        }

        @Override
        public void run() {
            String message;
            byte[] lmessage = new byte[MAX_UDP_DATAGRAM_LEN];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {
                while (keepRunning()) {
                    DatagramSocket dgramSocket = getSocket();
                    if (dgramSocket != null) {
                        dgramSocket.receive(packet);
                        message = new String(lmessage, 0, packet.getLength());
                        Log.e("DataGram", "Received msg " + message);
                        lastMessage = message;

                        String uri = "coap://"+packet.getAddress().getHostAddress()+":"+Integer.toString(packet.getPort())+"/ack";
                        rmInterface.sendRequest(uri, null, CA_IPV4, METHOD_GET, MSG_TYPE_ACK);
                    }
                }
            } catch (Throwable e) {
                if (keepRunning()) {
                    Log.e("DataGram", "Execption" + e);
                }
            }
            finally {
                close();
                RMInterfaceFactory.release(rmInterface);
            }
        }

        private synchronized DatagramSocket getSocket() throws SocketException {
            if ((socket == null) && bKeepRunning) {
                socket = new DatagramSocket(port, address);
            }
            return socket;
        }

        private synchronized boolean keepRunning() {
            return bKeepRunning;
        }

        private synchronized void close() {
            if ((socket != null) && !socket.isClosed()) {
                socket.close();
            }
            socket = null;
        }

        public synchronized void kill() {
            bKeepRunning = false;
            close();
        }

        public String getLastMessage() {
            return lastMessage;
        }
    }

}
