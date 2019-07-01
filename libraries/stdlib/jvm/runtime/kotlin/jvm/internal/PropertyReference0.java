/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KCallable;
import kotlin.reflect.KProperty0;

public abstract class PropertyReference0 extends PropertyReference implements KProperty0 {
    public PropertyReference0() {
        super();
    }

    @SinceKotlin(version = "1.1")
    public PropertyReference0(Object receiver) {
        super(receiver);
    }

    @Override
    protected KCallable computeReflected() {
        return Reflection.property0(this);
    }

    @Override
    public Object invoke() {
        return get();
    }

    @Override
    public KProperty0.Getter getGetter() {
        return ((KProperty0) getReflected()).getGetter();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public Object getDelegate() {
        return ((KProperty0) getReflected()).getDelegate();
    }
}
