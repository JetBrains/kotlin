// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dependency.GraphDataInput;
import org.jetbrains.jps.dependency.GraphDataOutput;
import org.jetbrains.jps.dependency.Usage;

import java.io.IOException;

abstract class JvmElementUsage implements Usage {

    private final @NotNull JvmNodeReferenceID myOwner;

    JvmElementUsage(@NotNull JvmNodeReferenceID owner) {
        myOwner = owner;
    }

    JvmElementUsage(GraphDataInput in) throws IOException {
        myOwner = new JvmNodeReferenceID(in);
    }

    @Override
    public void write(GraphDataOutput out) throws IOException {
        myOwner.write(out);
    }

    @Override
    public @NotNull JvmNodeReferenceID getElementOwner() {
        return myOwner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JvmElementUsage jvmUsage = (JvmElementUsage)o;

        if (!myOwner.equals(jvmUsage.myOwner)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return myOwner.hashCode();
    }
}
