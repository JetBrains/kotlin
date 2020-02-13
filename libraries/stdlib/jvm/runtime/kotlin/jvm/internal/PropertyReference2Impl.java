/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.reflect.KDeclarationContainer;

@SuppressWarnings({"unused", "NullableProblems"})
public class PropertyReference2Impl extends PropertyReference2 {
    private final KDeclarationContainer owner;
    private final String name;
    private final String signature;

    public PropertyReference2Impl(KDeclarationContainer owner, String name, String signature) {
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

    @Override
    public Object get(Object receiver1, Object receiver2) {
        return getGetter().call(receiver1, receiver2);
    }
}
