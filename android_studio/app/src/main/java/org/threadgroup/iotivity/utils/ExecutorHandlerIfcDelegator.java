/*
 * Copyright (c) 2013 K–NFB Reading Technology, Inc.
 */

package org.threadgroup.iotivity.utils;

import android.support.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * Allows calls to an interface's implementation to be run on an ExecutorService's threads.
 *
 *      MyInterface delegate  = new MyInterfaceImpl();
 *      MyInterface delegator = ExecutorHandlerIfcDelegator.get(delegate, executorService, listener);
 *      ...
 *      ...
 *      delegator.someMethod(a,b,c); // The 'someMethod' method of MyInterfaceImpl will be called on a background thread.
 *      ...
 *
 *
 * @author Anton Spaans on 1/9/2015.
 *
 * @param <Ifc> The target interface.
 */
public class ExecutorHandlerIfcDelegator<Ifc> extends IfcDelegator<Ifc> {

    /**
     * Returns a delegator that will run all calls to it on an AsyncTask's background thread.
     *
     * @param delegate The instance implementing the interface.
     * @param executorService The service that will execute the methods of the delegate.
     * @param resultListener If not null, the callback notifying the caller of method-call results.
     * @param <Ifc> The target interface.
     * @return The delegator.
     */
    @SuppressWarnings("unused")
    public static <Ifc> Ifc get(@NonNull Ifc delegate, @NonNull ExecutorService executorService, ResultListener resultListener) {
        ExecutorHandlerIfcDelegator<Ifc> delegator = new ExecutorHandlerIfcDelegator<>(delegate, executorService, resultListener);
        return delegator.get();
    }

    @SuppressWarnings("unused")
    public static <Ifc> Ifc get(@NonNull IfcReference<Ifc> reference, @NonNull ExecutorService executorService, ResultListener resultListener) {
        ExecutorHandlerIfcDelegator<Ifc> delegator = new ExecutorHandlerIfcDelegator<>(reference, executorService, resultListener);
        return delegator.get();
    }

    private final ResultListener mResultListener;
    private final ExecutorService mExecutorService;

    private ExecutorHandlerIfcDelegator(@NonNull Ifc delegate, @NonNull ExecutorService executorService, ResultListener resultListener) {
        super(delegate);
        mResultListener = resultListener;
        mExecutorService = executorService;
    }

    private ExecutorHandlerIfcDelegator(@NonNull IfcReference<Ifc> reference, @NonNull ExecutorService executorService, ResultListener resultListener) {
        super(reference);
        mResultListener = resultListener;
        mExecutorService = executorService;
    }

    @Override
    protected Object runOnDelegate(@NonNull final Method method, @NonNull final Ifc delegate, final Object[] args) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                doRun(delegate, method, args);
            }
        });

        return null;
    }

    @Override
    protected Object runOnDelegate(@NonNull final Method method, @NonNull final IfcReference<Ifc> reference, final Object[] args) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                doRun(reference.get(), method, args);
            }
        });

        return null;
    }

    private void doRun(Object delegate, Method method, Object[] args) {
        try {
            final Object result = method.invoke(delegate, args);
            if (mResultListener != null) {
                mResultListener.onResult(method, result, null);
            }
        }
        catch (Throwable t) {
            if (mResultListener != null) {
                mResultListener.onResult(method, null, t);
            }
        }
    }
}
