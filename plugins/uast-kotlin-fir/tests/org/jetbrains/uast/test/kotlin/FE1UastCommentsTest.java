/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

@TestMetadata("plugins/uast-kotlin-fir/testData/declaration")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class FE1UastCommentsTest extends AbstractFE1UastCommentsTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("facade.kt")
    public void testFacade() throws Exception {
        runTest("plugins/uast-kotlin-fir/testData/declaration/facade.kt");
    }

    @TestMetadata("objects.kt")
    public void testObjects() throws Exception {
        runTest("plugins/uast-kotlin-fir/testData/declaration/objects.kt");
    }
}
