/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;

import java.io.File;
import java.io.IOException;

public abstract class AbstractInheritorsSearchTest extends AbstractSearcherTest {
    public void doTest(@NotNull String path) throws IOException {
        checkClassWithDirectives(path);
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/search/inheritance").getPath() + File.separator;
    }
}
