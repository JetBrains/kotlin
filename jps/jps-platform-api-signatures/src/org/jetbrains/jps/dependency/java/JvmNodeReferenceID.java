// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dependency.GraphDataInput;
import org.jetbrains.jps.dependency.GraphDataOutput;
import org.jetbrains.jps.dependency.ReferenceID;

import java.io.IOException;

public final class JvmNodeReferenceID implements ReferenceID {
    private final String myName;

    public JvmNodeReferenceID(@NotNull String name) {
        myName = name;
    }

    public JvmNodeReferenceID(GraphDataInput in) throws IOException {
        myName = in.readUTF();
    }

    @Override
    public void write(GraphDataOutput out) throws IOException {
        out.writeUTF(myName);
    }

    /**
     * @return either JVM class name (FQ-name) or JVM module name
     */
    public String getNodeName() {
        return myName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JvmNodeReferenceID that = (JvmNodeReferenceID)o;

        if (!myName.equals(that.myName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return myName.hashCode();
    }

    @Override
    public String toString() {
        return "JVM_NODE_ID:" + myName;
    }
}
