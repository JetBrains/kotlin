// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency;

import org.jetbrains.annotations.NotNull;

/**
 * A base class describing a "usage" of some graph Node or one of the Node's internal elements
 */
public interface Usage extends ExternalizableGraphElement {

    /**
     * @return the ID referring to the Node being used or the Node, to which the used element belongs to
     */
    @NotNull
    ReferenceID getElementOwner();

    boolean equals(Object other);

    int hashCode();
}
