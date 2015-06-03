package org.threadgroup.iotivity.activities;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import org.iotivity.ca.service.RMInterfaceFactory;
import org.iotivity.ca.service.RMInterfaceIfc;
import org.iotivity.ca.service.RMResponseReceivedIfc;
import org.threadgroup.iotivity.R;

/**
 *
 * Created by Anton Spaans on 5/29/15.
 */
public class PrototypeActivity extends AppCompatActivity implements SendRequestFragment.Callback, DnsDiscovery.Callback {
    private static int CA_IPV4 = (1 << 0);
    private static int CA_IPV6 = (1 << 1);
    private static int CA_EDR = (1 << 2);
    private static int CA_LE = (1 << 3);

    DnsService dnsService = new DnsService();
    DnsDiscovery dnsDiscovery = new DnsDiscovery();

    RMInterfaceIfc rmInterface;

    String rmURI;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        dnsService.register(this);
        dnsDiscovery.startDiscovery(this);

        rmInterface = RMInterfaceFactory.obtain(this);
        RMInterfaceFactory.setResponseListener(new RMResponseReceivedIfc() {
            @Override
            public void OnResponseReceived(String subject, String receivedData) {
            }
        });
        rmInterface.RMSelectNetwork(CA_IPV4);
        rmInterface.RMHandleRequestResponse();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_prototype);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.container);
        if (fragment == null) {
            fragment = new SendRequestFragment();
            fragmentManager.beginTransaction().add(R.id.container, fragment).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        RMInterfaceFactory.setResponseListener(null);
        RMInterfaceFactory.release(rmInterface);

        dnsDiscovery.stopDiscovery();
        dnsService.unregister();
    }

    @Override
    public RMInterfaceIfc getRMInterface() {
        return rmInterface;
    }

    @Override
    public String getBaseUri() {
        synchronized (this) {
            return rmURI;
        }
    }

    @Override
    public void onServiceDiscovered(NsdServiceInfo serviceInfo) {
        if ("ThreadGroupNetAndroid1".equals(serviceInfo.getServiceName())) {
            synchronized (this) {
                rmURI = "coap://" + serviceInfo.getHost().getHostAddress() + ":" + Integer.toString(serviceInfo.getPort());
                rmURI = "coap://10.1.10.110:5683";
            }
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        if ("ThreadGroupNetAndroid1".equals(serviceInfo.getServiceName())) {
            synchronized (this) {
                rmURI = null;
            }
        }
    }
}
