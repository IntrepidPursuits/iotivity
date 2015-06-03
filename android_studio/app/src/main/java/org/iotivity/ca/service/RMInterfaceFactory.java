package org.iotivity.ca.service;

import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import org.threadgroup.iotivity.utils.ExecutorHandlerIfcDelegator;
import org.threadgroup.iotivity.utils.IfcDelegator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 *
 * Created by Anton Spaans on 5/29/15.
 */
public class RMInterfaceFactory {
    private static volatile Context sContext;
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(new RMThreadFactory());

    static RMResponseReceivedIfc RESPONSE_LISTENER = null;

    static SparseArray<RMInterfaceReference> OBTAINED_INTERFACES = new SparseArray<>();

    public static RMInterfaceIfc obtain(Context context) {
        if (sContext == null) {
            synchronized (RMInterfaceFactory.class) {
                if (sContext == null) {
                    sContext = context.getApplicationContext();
                }
            }
        }

        RMInterfaceReference reference = new RMInterfaceReference();
        RMInterfaceIfc ifc = ExecutorHandlerIfcDelegator.get(reference, EXECUTOR_SERVICE, null);

        OBTAINED_INTERFACES.put(System.identityHashCode(ifc), reference);

        return ifc;
    }

    public static void release(RMInterfaceIfc ifc) {
        int key = System.identityHashCode(ifc);
        final RMInterfaceReference reference = OBTAINED_INTERFACES.get(key);
        if (reference != null) {
            OBTAINED_INTERFACES.remove(key);
            EXECUTOR_SERVICE.execute(new Runnable() {
                @Override
                public void run() {
                    reference.release();
                }
            });
        }
    }

    public static synchronized void setResponseListener(RMResponseReceivedIfc listener) {
        synchronized (RMInterfaceFactory.class) {
            RESPONSE_LISTENER = listener;
        }
    }

    static void notifyReponseListener(String subject, String data) {
        final RMResponseReceivedIfc listener;
        synchronized (RMInterfaceFactory.class) {
            listener = RESPONSE_LISTENER;
        }

        if (listener != null) {
            listener.OnResponseReceived(subject, data);
        }
    }

    private static class LoaderImpl {
        private static RMInterface IMPL;
        private static int REF_COUNT;

        static synchronized RMInterface obtainImpl(Context context) {
            if (IMPL == null) {
                IMPL = new RMInterface(context);
            }
            REF_COUNT++;
            return IMPL;
        }

        static synchronized void releaseImpl() {
            if (IMPL == null) {
                REF_COUNT = 0;
                return;
            }

            REF_COUNT--;
            if (REF_COUNT == 0) {
                IMPL.close();
                IMPL = null;
            }
        }
    }

    private static class RMThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NonNull final Runnable r) {
            return new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    r.run();
                }
            });
        }
    }

    private static class RMInterfaceReference implements IfcDelegator.IfcReference<RMInterfaceIfc> {
        private RMInterfaceIfc rmInterface;

        @Override
        public @NonNull Class<RMInterfaceIfc> getClazz() {
            return RMInterfaceIfc.class;
        }

        @Override
        public @NonNull RMInterfaceIfc get() {
            if (rmInterface == null) {
                rmInterface = LoaderImpl.obtainImpl(sContext);
            }
            return rmInterface;
        }

        void release() {
            if (rmInterface != null) {
                LoaderImpl.releaseImpl();
                rmInterface = null;
            }
        }
    }
}
