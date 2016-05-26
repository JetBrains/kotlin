/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
/*
package org.jetbrains.kotlin.jps.build.android;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

@SuppressWarnings("all")
@TestMetadata("plugins/android-extensions/android-extensions-jps/testData/android")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class AndroidJpsTestCaseGenerated extends AbstractAndroidJpsTestCase {
    public void testAllFilesPresentInAndroid() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("plugins/android-extensions/android-extensions-jps/testData/android"), Pattern.compile("^([^\\.]+)$"), TargetBackend.ANY, false);
    }

    @TestMetadata("simple")
    public void testSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("plugins/android-extensions/android-extensions-jps/testData/android/simple/");
        doTest(fileName);
    }
}
*/