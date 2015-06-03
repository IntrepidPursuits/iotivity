package org.threadgroup.iotivity.activities;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by aspaans on 6/2/15.
 */
public class DnsResolver implements NsdManager.ResolveListener {
    private static volatile DnsResolver RESOLVER;

    static DnsResolver get(Context context) {
        if (RESOLVER == null) {
            synchronized (DnsResolver.class) {
                if (RESOLVER == null) {
                    RESOLVER = new DnsResolver(context.getApplicationContext());
                }
            }
        }
        return RESOLVER;
    }

    private final NsdManager mNsdManager;
    private final Set<String> servicesBeingResolved = new HashSet<>();
    private final List<NsdManager.ResolveListener> listeners = new ArrayList<>();

    private DnsResolver(Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    void resolve(NsdServiceInfo serviceInfo) {
        if (!servicesBeingResolved.contains(serviceInfo.getServiceName())) {
            servicesBeingResolved.add(serviceInfo.getServiceName());
            mNsdManager.resolveService(serviceInfo, this);
        }
    }

    @Override
    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        servicesBeingResolved.remove(serviceInfo.getServiceName());

        if (listeners.isEmpty()) {
            return;
        }

        final List<NsdManager.ResolveListener> list;
        synchronized (listeners) {
            list = new ArrayList<>(listeners);
        }

        for (NsdManager.ResolveListener resolveListener : list) {
            resolveListener.onResolveFailed(serviceInfo, errorCode);
        }
    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        servicesBeingResolved.remove(serviceInfo.getServiceName());

        if (listeners.isEmpty()) {
            return;
        }

        final List<NsdManager.ResolveListener> list;
        synchronized (listeners) {
            list = new ArrayList<>(listeners);
        }

        for (NsdManager.ResolveListener resolveListener : list) {
            resolveListener.onServiceResolved(serviceInfo);
        }
    }

    void addListener(NsdManager.ResolveListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    void removeListener(NsdManager.ResolveListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
}


