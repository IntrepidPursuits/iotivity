package org.threadgroup.iotivity.activities;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by aspaans on 6/2/15.
 */
class DnsDiscovery {

    interface Callback {
        void onServiceDiscovered(NsdServiceInfo serviceInfo);
        void onServiceLost(NsdServiceInfo serviceInfo);
    }

    Handler handler = new Handler();

    Callback callback;
    NsdManager mNsdManager;
    DnsResolver resolver;

    private NsdManager.DiscoveryListener mListener = new NsdManager.DiscoveryListener() {
        Set<String> beingResolved = new HashSet<>();

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e("DnsDiscovery", "Start failed");
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e("DnsDiscovery", "Stop failed");
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.e("DnsDiscovery", "Started");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.e("DnsDiscovery", "Stopped");
        }

        @Override
        public void onServiceFound(final NsdServiceInfo serviceInfo) {
            Log.e("DnsDiscovery", "Service found "+serviceInfo);
            resolver.resolve(serviceInfo);

//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
//                        @Override
//                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
//                            Log.e("DnsDiscovery", "Resolved Failed " + serviceInfo.toString() + "; error: " + errorCode);
//                            beingResolved.remove(serviceInfo.getServiceName());
//                        }
//
//                        @Override
//                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
//                            Log.e("DnsDiscovery", "Resolve succeeded " + serviceInfo.toString());
//
//                            if (callback != null) {
//                                callback.onServiceDiscovered(serviceInfo);
//                            }
//                            beingResolved.remove(serviceInfo.getServiceName());
//                        }
//                    };
//                    if (!beingResolved.contains(serviceInfo.getServiceName())) {
//                        beingResolved.add(serviceInfo.getServiceName());
//                        mNsdManager.resolveService(serviceInfo, resolveListener);
//                    }
//                }
//            }, 0);
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.e("DnsDiscovery", "Service Lost");
            if (callback != null) {
                callback.onServiceLost(serviceInfo);
            }
            // beingResolved.remove(serviceInfo.getServiceName());
        }
    };

    NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e("DnsDiscovery", "Resolved Failed " + serviceInfo.toString() + "; error: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e("DnsDiscovery", "Resolve succeeded " + serviceInfo.toString());

            if (callback != null) {
                callback.onServiceDiscovered(serviceInfo);
            }
        }
    };

    public void startDiscovery(Context context) {
        if (context instanceof Callback) {
            callback = (Callback) context;
        }
        if (resolver == null) {
            resolver = DnsResolver.get(context);
        }

        resolver.addListener(resolveListener);
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices("_thread-net._udp", NsdManager.PROTOCOL_DNS_SD, mListener);
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mListener);
        resolver.removeListener(resolveListener);
        callback = null;
    }
}
