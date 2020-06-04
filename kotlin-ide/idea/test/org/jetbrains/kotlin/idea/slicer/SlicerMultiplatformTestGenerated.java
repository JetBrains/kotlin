/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.test.TestRoot;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

@SuppressWarnings("all")
@TestRoot("idea")
@TestMetadata("testData/slicer/mpp")
@TestDataPath("$PROJECT_ROOT")
public class SlicerMultiplatformTestGenerated extends AbstractSlicerMultiplatformTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("actualClassFunctionParameter")
    public void testActualClassFunctionParameter() throws Exception {
        runTest("testData/slicer/mpp/actualClassFunctionParameter/");
    }

    @TestMetadata("actualFunctionParameter")
    public void testActualFunctionParameter() throws Exception {
        runTest("testData/slicer/mpp/actualFunctionParameter/");
    }

    public void testAllFilesPresentInMpp() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("idea/testData/slicer/mpp"), Pattern.compile("^([^\\.]+)$"), null, false);
    }

    @TestMetadata("expectClassFunctionParameter")
    public void testExpectClassFunctionParameter() throws Exception {
        runTest("testData/slicer/mpp/expectClassFunctionParameter/");
    }

    @TestMetadata("expectExtensionFunctionResultOut")
    public void testExpectExtensionFunctionResultOut() throws Exception {
        runTest("testData/slicer/mpp/expectExtensionFunctionResultOut/");
    }

    @TestMetadata("expectFunctionParameter")
    public void testExpectFunctionParameter() throws Exception {
        runTest("testData/slicer/mpp/expectFunctionParameter/");
    }

    @TestMetadata("expectFunctionResultIn")
    public void testExpectFunctionResultIn() throws Exception {
        runTest("testData/slicer/mpp/expectFunctionResultIn/");
    }

    @TestMetadata("expectFunctionResultOut")
    public void testExpectFunctionResultOut() throws Exception {
        runTest("testData/slicer/mpp/expectFunctionResultOut/");
    }

    @TestMetadata("expectPropertyResultIn")
    public void testExpectPropertyResultIn() throws Exception {
        runTest("testData/slicer/mpp/expectPropertyResultIn/");
    }

    @TestMetadata("expectPropertyResultOut")
    public void testExpectPropertyResultOut() throws Exception {
        runTest("testData/slicer/mpp/expectPropertyResultOut/");
    }
}
