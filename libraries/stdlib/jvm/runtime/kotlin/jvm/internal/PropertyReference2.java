/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KCallable;
import kotlin.reflect.KProperty2;

@SuppressWarnings("unchecked")
public abstract class PropertyReference2 extends PropertyReference implements KProperty2 {
    @Override
    protected KCallable computeReflected() {
        return Reflection.property2(this);
    }

    @Override
    public Object invoke(Object receiver1, Object receiver2) {
        return get(receiver1, receiver2);
    }

    @Override
    public KProperty2.Getter getGetter() {
        return ((KProperty2) getReflected()).getGetter();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public Object getDelegate(Object receiver1, Object receiver2) {
        return ((KProperty2) getReflected()).getDelegate(receiver1, receiver2);
    }
}
