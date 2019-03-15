/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractReferenceToJavaWithWrongFileStructureTest extends AbstractReferenceResolveTest {
    @Override
    protected void doTest(@NotNull String path) {
        //this line intentionally creates wrong file structure for java file
        myFixture.configureByFile(path.replace(".kt", ".java"));
        myFixture.configureByFile(path);
        performChecks();
    }
}
