/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.*;

@SuppressWarnings({"rawtypes", "unused", "NullableProblems"})
public abstract class MutablePropertyReference0 extends MutablePropertyReference implements KMutableProperty0 {
    public MutablePropertyReference0() {
    }

    @SinceKotlin(version = "1.1")
    public MutablePropertyReference0(Object receiver) {
        super(receiver);
    }

    @SinceKotlin(version = "1.4")
    public MutablePropertyReference0(Object receiver, Class owner, String name, String signature, int flags) {
        super(receiver, owner, name, signature, flags);
    }

    @Override
    protected KCallable computeReflected() {
        return Reflection.mutableProperty0(this);
    }

    @Override
    public Object invoke() {
        return get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public KProperty0.Getter getGetter() {
        return syntheticJavaProperty ? new SyntheticJavaPropertyReference0Getter(this) : ((KMutableProperty0) getReflected()).getGetter();
    }

    @Override
    @SuppressWarnings("unchecked")
    public KMutableProperty0.Setter getSetter() {
        return syntheticJavaProperty ? new SyntheticJavaPropertyReference0Setter(this) : ((KMutableProperty0) getReflected()).getSetter();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public Object getDelegate() {
        return ((KMutableProperty0) getReflected()).getDelegate();
    }
}
