/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KFunction;

import java.io.Serializable;

/**
 * Superclass for instances of functional interface constructor references:
 * <pre>
 *     fun interface IFoo {
 *         fun foo()
 *     }
 *     val iFoo = IFoo { println("Hello!") }    // calling fun interface constructor
 *     val iFooCtor = ::IFoo                    // callable reference to fun interface constructor
 * </pre>
 *
 * Doesn't support reflection yet.
 */
@SuppressWarnings({"rawtypes", "WeakerAccess", "unused"})
@SinceKotlin(version = "1.7")
public class FunInterfaceConstructorReference extends FunctionReference implements Serializable {
    private final Class funInterface;

    public FunInterfaceConstructorReference(Class funInterface) {
        super(1);
        this.funInterface = funInterface;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FunInterfaceConstructorReference)) return false;
        FunInterfaceConstructorReference other = (FunInterfaceConstructorReference) o;
        return funInterface.equals(other.funInterface);
    }

    @Override
    public int hashCode() {
        return funInterface.hashCode();
    }

    @Override
    public String toString() {
        return "fun interface " + funInterface.getName();
    }

    @Override
    protected KFunction getReflected() {
        throw new UnsupportedOperationException("Functional interface constructor does not support reflection");
    }
}
