/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KCallable;
import kotlin.reflect.KFunction;

@SuppressWarnings({"rawtypes", "unused"})
public class FunctionReference extends CallableReference implements FunctionBase, EquatableKFunction {
    private final int arity;

    public FunctionReference(int arity) {
        this(arity, NO_RECEIVER, null, null, null, 0);
    }

    @SinceKotlin(version = "1.1")
    public FunctionReference(int arity, Object receiver) {
        this(arity, receiver, null, null, null, 0);
    }

    @SinceKotlin(version = "1.4")
    public FunctionReference(int arity, Object receiver, Class owner, String name, String signature, int flags) {
        super(receiver, owner, name, signature, flags);
        this.arity = arity;
    }

    @Override
    public int getArity() {
        return arity;
    }

    @Override
    @SinceKotlin(version = "1.1")
    protected KFunction getReflected() {
        return (KFunction) super.getReflected();
    }

    @Override
    @SinceKotlin(version = "1.1")
    protected KCallable computeReflected(boolean forceStdlibOnlyReflection) {
        return forceStdlibOnlyReflection ? StdlibOnlyReflection.function(this) : Reflection.function(this);
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isInline() {
        return getReflected().isInline();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isExternal() {
        return getReflected().isExternal();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isOperator() {
        return getReflected().isOperator();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isInfix() {
        return getReflected().isInfix();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isSuspend() {
        return getReflected().isSuspend();
    }

    @Override
    @SinceKotlin(version = "2.4")
    public Object getRawBoundReceiver() {
        return receiver;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        // the only KFunction without equality data are properties getters/setters, and they are not equal to functions
        if (!(obj instanceof EquatableKFunction)) return false;

        EquatableKFunction other = (EquatableKFunction) obj;
        return getName().equals(other.getName()) &&
               getSignature().equals(other.getSignature()) &&
               Intrinsics.areEqual(getRawBoundReceiver(), other.getRawBoundReceiver()) &&
               Intrinsics.areEqual(getOwner(), other.getOwner());
    }

    @Override
    public int hashCode() {
        return ((getOwner() == null ? 0 : getOwner().hashCode() * 31) + getName().hashCode()) * 31 + getSignature().hashCode();
    }

    @Override
    public String toString() {
        KCallable reflected = compute();
        if (reflected != this) {
            return reflected.toString();
        }

        // TODO: consider adding the class name to toString() for constructors
        return "<init>".equals(getName())
               ? "constructor" + Reflection.REFLECTION_NOT_AVAILABLE
               : "function " + getName() + Reflection.REFLECTION_NOT_AVAILABLE;
    }
}
