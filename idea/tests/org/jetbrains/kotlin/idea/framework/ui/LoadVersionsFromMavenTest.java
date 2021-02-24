/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework.ui;

import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.junit.runner.RunWith;

import java.util.Collection;

@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class LoadVersionsFromMavenTest extends LightIdeaTestCase {
    public void testDownload() throws Exception {
        Collection<String> versions = ConfigureDialogWithModulesAndVersion.loadVersions("1.0.0");
        assertTrue(versions.size() > 0);
        for (String version : versions) {
            assertTrue(VersionComparatorUtil.compare(version, "1.0.0") >= 0);
        }
    }
}
