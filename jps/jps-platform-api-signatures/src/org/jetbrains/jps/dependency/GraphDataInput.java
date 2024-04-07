// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.dependency;

import java.io.DataInput;
import java.io.IOException;
import java.util.Collection;

public interface GraphDataInput extends DataInput {

    <T extends ExternalizableGraphElement> T readGraphElement() throws IOException;

    <T extends ExternalizableGraphElement, C extends Collection<? super T>> C readGraphElementCollection(C acc) throws IOException;
}
