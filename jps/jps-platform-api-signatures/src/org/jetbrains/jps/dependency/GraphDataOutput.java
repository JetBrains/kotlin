// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;

public interface GraphDataOutput extends DataOutput {

    <T extends ExternalizableGraphElement> void writeGraphElement(@NotNull T elem) throws IOException;

    <T extends ExternalizableGraphElement> void writeGraphElementCollection(Class<? extends T> elemType, @NotNull Iterable<T> col) throws IOException;
}
