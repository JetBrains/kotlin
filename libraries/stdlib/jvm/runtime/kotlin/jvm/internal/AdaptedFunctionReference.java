/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KDeclarationContainer;

import java.io.Serializable;

import static kotlin.jvm.internal.CallableReference.NO_RECEIVER;

/**
 * Superclass for instances of adapted function references, i.e. references where expected function type differs
 * from the target function signature: <pre>
 *     fun target(s: String? = ""): String = s!!
 *     fun use(f: () -> Unit) {}
 *     use(::target)  // adapted function reference (default argument conversion + coercion to Unit)
 * </pre>
 *
 * It doesn't inherit from {@link FunctionReference} because such references don't support reflection yet.
 * Once this changes in the future, the JVM codegen may simply use {@link FunctionReferenceImpl}
 * for adapted function references instead of this class.
 */
@SuppressWarnings({"rawtypes", "WeakerAccess", "unused"})
@SinceKotlin(version = "1.4")
public class AdaptedFunctionReference implements FunctionBase, Serializable {
    protected final Object receiver;
    private final Class owner;
    private final String name;
    private final String signature;
    private final boolean isTopLevel;
    private final int arity;
    private final int flags;

    public AdaptedFunctionReference(int arity, Class owner, String name, String signature, int flags) {
        this(arity, NO_RECEIVER, owner, name, signature, flags);
    }

    public AdaptedFunctionReference(int arity, Object receiver, Class owner, String name, String signature, int flags) {
        this.receiver = receiver;
        this.owner = owner;
        this.name = name;
        this.signature = signature;
        this.isTopLevel = (flags & 1) == 1;
        this.arity = arity;
        this.flags = flags >> 1;
    }

    @Override
    public int getArity() {
        return arity;
    }

    public KDeclarationContainer getOwner() {
        return owner == null ? null :
               isTopLevel ? Reflection.getOrCreateKotlinPackage(owner) : Reflection.getOrCreateKotlinClass(owner);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdaptedFunctionReference)) return false;
        AdaptedFunctionReference other = (AdaptedFunctionReference) o;
        return isTopLevel == other.isTopLevel &&
               arity == other.arity &&
               flags == other.flags &&
               Intrinsics.areEqual(receiver, other.receiver) &&
               Intrinsics.areEqual(owner, other.owner) &&
               name.equals(other.name) &&
               signature.equals(other.signature);
    }

    @Override
    public int hashCode() {
        int result = receiver != null ? receiver.hashCode() : 0;
        result = result * 31 + (owner != null ? owner.hashCode() : 0);
        result = result * 31 + name.hashCode();
        result = result * 31 + signature.hashCode();
        result = result * 31 + (isTopLevel ? 1231 : 1237);
        result = result * 31 + arity;
        result = result * 31 + flags;
        return result;
    }

    @Override
    public String toString() {
        return Reflection.renderLambdaToString(this);
    }
}
