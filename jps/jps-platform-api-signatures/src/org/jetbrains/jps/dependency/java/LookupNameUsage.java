// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency.java;

import org.jetbrains.jps.dependency.GraphDataInput;

import java.io.IOException;

public final class LookupNameUsage extends MemberUsage {

    public LookupNameUsage(String className, String name) {
        super(className, name);
    }

    public LookupNameUsage(JvmNodeReferenceID clsId, String name) {
        super(clsId, name);
    }

    public LookupNameUsage(GraphDataInput in) throws IOException {
        super(in);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 2;
    }
}
