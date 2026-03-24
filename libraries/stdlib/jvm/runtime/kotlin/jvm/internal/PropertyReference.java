/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KCallable;
import kotlin.reflect.KProperty;

@SuppressWarnings("rawtypes")
public abstract class PropertyReference extends CallableReference implements EquatableKProperty {

    public PropertyReference() {
        super();
    }

    @SinceKotlin(version = "1.1")
    public PropertyReference(Object receiver) {
        super(receiver);
    }

    @SinceKotlin(version = "1.4")
    public PropertyReference(Object receiver, Class owner, String name, String signature, int flags) {
        super(receiver, owner, name, signature, flags);
    }

    private boolean isSyntheticJavaProperty() {
        return Flag.SYNTHETIC_JAVA_PROPERTY.isSet(getFlags());
    }

    @Override
    @SinceKotlin(version = "1.1")
    protected KProperty getReflected() {
        if (isSyntheticJavaProperty()) {
            throw new UnsupportedOperationException("Kotlin reflection is not yet supported for synthetic Java properties. " +
                                                    "Please follow/upvote https://youtrack.jetbrains.com/issue/KT-55980");
        }
        return (KProperty) super.getReflected();
    }

    @Override
    public KCallable compute() {
        return isSyntheticJavaProperty() ? this : super.compute();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isLateinit() {
        return getReflected().isLateinit();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isConst() {
        return getReflected().isConst();
    }

    @Override
    @SinceKotlin(version = "2.4")
    public Object getRawBoundReceiver() {
        return receiver;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof EquatableKProperty)) return false;

        EquatableKProperty other = (EquatableKProperty) obj;
        return getOwner().equals(other.getOwner()) &&
               getName().equals(other.getName()) &&
               getSignature().equals(other.getSignature()) &&
               Intrinsics.areEqual(getRawBoundReceiver(), other.getRawBoundReceiver());
    }

    @Override
    public int hashCode() {
        return (getOwner().hashCode() * 31 + getName().hashCode()) * 31 + getSignature().hashCode();
    }

    @Override
    public String toString() {
        KCallable reflected = compute();
        if (reflected != this) {
            return reflected.toString();
        }

        return "property " + getName() + Reflection.REFLECTION_NOT_AVAILABLE;
    }
}
