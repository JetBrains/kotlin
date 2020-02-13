/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.reflect.KDeclarationContainer;

@SuppressWarnings({"unused", "NullableProblems"})
public class FunctionReferenceImpl extends FunctionReference {
    private final KDeclarationContainer owner;
    private final String name;
    private final String signature;

    public FunctionReferenceImpl(int arity, KDeclarationContainer owner, String name, String signature) {
        super(arity);
        this.owner = owner;
        this.name = name;
        this.signature = signature;
    }

    @Override
    public KDeclarationContainer getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSignature() {
        return signature;
    }
}
