/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KClass;
import kotlin.reflect.KDeclarationContainer;

@SuppressWarnings({"unused", "rawtypes"})
public class FunctionReferenceImpl extends FunctionReference {
    public FunctionReferenceImpl(int arity, KDeclarationContainer owner, String name, String signature) {
        super(
                arity, NO_RECEIVER,
                ((ClassBasedDeclarationContainer) owner).getJClass(), name, signature,
                owner instanceof KClass ? 0 : 1
        );
    }

    @SinceKotlin(version = "1.4")
    public FunctionReferenceImpl(int arity, Class owner, String name, String signature, int flags) {
        super(arity, NO_RECEIVER, owner, name, signature, flags);
    }

    @SinceKotlin(version = "1.4")
    public FunctionReferenceImpl(int arity, Object receiver, Class owner, String name, String signature, int flags) {
        super(arity, receiver, owner, name, signature, flags);
    }
}
