/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.reflect.KDeclarationContainer;

public class PropertyReference1Impl extends PropertyReference1 {
    private final KDeclarationContainer owner;
    private final String name;
    private final String signature;

    public PropertyReference1Impl(KDeclarationContainer owner, String name, String signature) {
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
    public Object get(Object receiver) {
        return getGetter().call(receiver);
    }
}
