/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.lang.invoke.VarHandle;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;

public class SafePublicationLazyImpl<T> implements Serializable, Lazy<T> {
    private static final AtomicReferenceFieldUpdater<SafePublicationLazyImpl, Object> valueUpdater =
            AtomicReferenceFieldUpdater.newUpdater(SafePublicationLazyImpl.class, Object.class, "_value");

    private volatile Function0<? extends T> initializer;
    private volatile Object _value;

    public SafePublicationLazyImpl(Function0<? extends T> init) {
        Intrinsics.checkNotNullParameter(init, "initializer");
        initializer = init;
        _value = UNINITIALIZED_VALUE.INSTANCE;
        VarHandle.releaseFence();
    }

    public T getValue() {
        Object value = _value;
        if (value != UNINITIALIZED_VALUE.INSTANCE) {
            @SuppressWarnings("unchecked")
            T vc = (T) value;
            return vc;
        }

        Function0<? extends T> init = initializer;
        if (init != null) {
            @SuppressWarnings("unchecked")
            T newValue = init.invoke();
            if (valueUpdater.compareAndSet(this, UNINITIALIZED_VALUE.INSTANCE, newValue)) {
                initializer = null;
                return newValue;
            }
        }
        @SuppressWarnings("unchecked")
        T vc = (T) _value;
        return vc;
    }

    public boolean isInitialized() {
        return _value != UNINITIALIZED_VALUE.INSTANCE;
    }

    public String toString() {
        if (isInitialized()) {
            return getValue().toString();
        }
        return "Lazy value not initialized yet.";
    }
}
