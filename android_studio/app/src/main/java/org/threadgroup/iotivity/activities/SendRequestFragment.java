package org.threadgroup.iotivity.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.iotivity.ca.service.RMInterfaceIfc;
import org.threadgroup.iotivity.R;

/**
 *
 * Created by Anton Spaans on 5/29/15.
 */
public class SendRequestFragment extends Fragment {
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

    interface Callback {
        RMInterfaceIfc getRMInterface();
        String getBaseUri();
    }

    Callback callback;
    RMInterfaceIfc rmInterfaceIfc;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (Callback) activity;
        rmInterfaceIfc = callback.getRMInterface();
    }

    @Override
    public void onDetach() {
        callback = null;
        rmInterfaceIfc = null;
        super.onDetach();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setReenterTransition(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sendrequest, container, false);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uri = (callback != null)? callback.getBaseUri() : null;
                if (uri != null) {
                    uri = uri + "/light";
                    rmInterfaceIfc.sendRequest(uri, "Hello There Android!", CA_IPV4, METHOD_GET, MSG_TYPE_CON);
                }
            }
        });

        return view;
    }
}
