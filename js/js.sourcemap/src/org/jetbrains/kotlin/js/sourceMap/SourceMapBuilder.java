/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap;

import java.io.File;

public interface SourceMapBuilder extends SourceMapMappingConsumer {
    void skipLinesAtBeginning(int count);

    void addLink();

    File getOutFile();

    String build();
}
