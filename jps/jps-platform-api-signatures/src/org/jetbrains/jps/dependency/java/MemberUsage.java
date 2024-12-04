// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency.java;

import org.jetbrains.jps.dependency.GraphDataInput;
import org.jetbrains.jps.dependency.GraphDataOutput;

import java.io.IOException;

public abstract class MemberUsage extends JvmElementUsage {

    private final String myName;

    protected MemberUsage(String className, String name) {
        this(new JvmNodeReferenceID(className), name);
    }

    protected MemberUsage(JvmNodeReferenceID clsId, String name) {
        super(clsId);
        myName = name;
    }

    MemberUsage(GraphDataInput in) throws IOException {
        super(in);
        myName = in.readUTF();
    }

    @Override
    public void write(GraphDataOutput out) throws IOException {
        super.write(out);
        out.writeUTF(myName);
    }

    public String getName() {
        return myName;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        final MemberUsage that = (MemberUsage)o;

        if (!myName.equals(that.myName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + myName.hashCode();
    }
}
