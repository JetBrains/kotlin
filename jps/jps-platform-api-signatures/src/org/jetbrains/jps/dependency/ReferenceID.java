// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency;

/**
 * This is a data property of a Node, used to reference the Node by other Nodes.
 * For example, is a Node represents a java class, a full qualified name of the class can be used as the Node's ReferenceID
 */
public interface ReferenceID extends ExternalizableGraphElement {
    boolean equals(Object other);

    int hashCode();
}
