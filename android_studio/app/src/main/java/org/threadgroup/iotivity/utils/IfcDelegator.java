/*
 * Copyright (c) 2013 K–NFB Reading Technology, Inc.
 */

package org.threadgroup.iotivity.utils;

import android.support.annotation.NonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Instances of this class allow for calls to an interface to be invoked on a delegate.
 *
 * E.g.
 *      MyInterface delegate = ...;
 *      IfcDelegator delegator = new MyIfcDelegator(delegate);
 *
 *      MyInterface interfaceDelegator = delegator.get();
 *      interfaceDelegator.someMethod(a, b, c);
 *
 * This would cause the someMethod(a,b,c) of the 'delegate' to be called.
 *
 * This seems without any purpose; why not just call delegate.someMethod(a,b,c) directly?
 *
 * All calls to the methods of 'delegate' will be done through a call to {@link #runOnDelegate(java.lang.reflect.Method, Object, Object[])}.
 * This means you can intercept every call to 'delegate' and do some pre- or post-processing before/after the method on 'delegate' is called by
 * implementing {@link #runOnDelegate(java.lang.reflect.Method, Object, Object[])}. It is a poor man's AOP.
 *
 * Use cases are implementing a subclass to easily run interface methods on a UI thread or run interface methods on an AsyncTask's background-thread.
 *
 * @author Anton Spaans on 8/1/13.
 *
 * @param <Ifc> The target interface.
 */
public abstract class IfcDelegator<Ifc> implements InvocationHandler {

    /**
     * Implement this interface if you want to listen to the results of the
     * method calls to the delegate.
     */
    public interface ResultListener {
        /**
         * Called when the delegate method has been called that returns a result or threw an exception.
         * Note that it is called on the background thread.
         *
         * @param method Method that was called on the background thread.
         * @param result The result of the method call.
         * @param t      Not null if the call to 'method' resulted in an exception/error/throwable.
         */
        void onResult(Method method, Object result, Throwable t);
    }

    public interface IfcReference<Ifc> {
        @NonNull Class<Ifc> getClazz();
        @NonNull Ifc get();
    }

    private final Ifc        mDelegate;
    private final Class<?> mDelegateClass;
    private final IfcReference<Ifc> mReference;
    private final Class<?>[] mInterfaces;

    protected IfcDelegator(@NonNull Ifc delegate) {
        mDelegate      = delegate;
        mReference     = null;

        mDelegateClass = mDelegate.getClass();
        mInterfaces    = mDelegateClass.getInterfaces().clone();
    }

    protected IfcDelegator(@NonNull IfcReference<Ifc> reference) {
        mDelegate      = null;
        mReference     = reference;

        mDelegateClass = reference.getClazz();
        mInterfaces    = mDelegateClass.isInterface()
                ? new Class<?>[] { mDelegateClass }
                : mDelegateClass.getInterfaces().clone();
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ((mInterfaces == null) || (mInterfaces.length == 0)) {
            throw new IllegalStateException("Delegator doesn't implement the proper interfaces.");
        }

        for (Class<?> interfaze : mInterfaces) {
            if (hasMethod(interfaze, method)) {
                if (mDelegate != null) {
                    return runOnDelegate(method, mDelegate, args);
                } else if (mReference != null) {
                    return runOnDelegate(method, mReference, args);
                }
            }
            else {
                if (mDelegate != null) {
                    return method.invoke(mDelegate, args);
                } else if (mReference != null) {
                    return method.invoke(mReference, args);
                }
            }
        }

        throw new IllegalStateException("Delegator doesn't implement the proper interfaces.");
    }

    private boolean hasMethod(Class<?> interfaze, Method method) {
        try {
            return interfaze.getMethod(method.getName(), method.getParameterTypes()) != null;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    /**
     * @return A delegator for the target interface.
     */
    @SuppressWarnings("unchecked")
    public final Ifc get() {
        return (Ifc) Proxy.newProxyInstance(mDelegateClass.getClassLoader(), mInterfaces, this);
    }

    /**
     * Implement this method to do some pre- or post-processing before calling
     * the method on the delegate with the given args.
     * @param method The method to call.
     * @param delegate The delegate on which the method should be called.
     * @param args The arguments of the method call.
     * @return The result of the method call.
     */
    protected abstract Object runOnDelegate(@NonNull Method method, @NonNull Ifc delegate, Object[] args);

    protected Object runOnDelegate(@NonNull Method method, @NonNull IfcReference<Ifc> reference, Object[] args) {
        return runOnDelegate(method, reference.get(), args);
    }
}
