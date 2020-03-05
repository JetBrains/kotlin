/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KCallable;
import kotlin.reflect.KMutableProperty2;
import kotlin.reflect.KProperty2;

@SuppressWarnings({"unchecked", "rawtypes", "NullableProblems"})
public abstract class MutablePropertyReference2 extends MutablePropertyReference implements KMutableProperty2 {
    @Override
    protected KCallable computeReflected() {
        return Reflection.mutableProperty2(this);
    }

    @Override
    public Object invoke(Object receiver1, Object receiver2) {
        return get(receiver1, receiver2);
    }

    @Override
    public KProperty2.Getter getGetter() {
        return ((KMutableProperty2) getReflected()).getGetter();
    }

    @Override
    public KMutableProperty2.Setter getSetter() {
        return ((KMutableProperty2) getReflected()).getSetter();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public Object getDelegate(Object receiver1, Object receiver2) {
        return ((KMutableProperty2) getReflected()).getDelegate(receiver1, receiver2);
    }
}
