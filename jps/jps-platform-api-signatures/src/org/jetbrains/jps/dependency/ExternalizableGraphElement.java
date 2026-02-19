// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency;

import java.io.IOException;

public interface ExternalizableGraphElement {

    void write(GraphDataOutput out) throws IOException;
}
