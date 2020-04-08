/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation;

import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.ide.util.gotoByName.GotoSymbolModel2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import static org.jetbrains.kotlin.idea.navigation.GotoCheck.checkGotoDirectives;

public abstract class AbstractKotlinGotoTest extends KotlinLightCodeInsightFixtureTestCase {
    protected void doSymbolTest(String path) {
        myFixture.configureByFile(fileName());
        checkGotoDirectives(new GotoSymbolModel2(getProject()), myFixture.getEditor());
    }

    protected void doClassTest(String path) {
        myFixture.configureByFile(fileName());
        checkGotoDirectives(new GotoClassModel2(getProject()), myFixture.getEditor());
    }

    private String dirPath = null;

    @Override
    protected void setUp() {
        dirPath = KotlinTestUtils.getTestsRoot(getClass());
        super.setUp();
    }

    @Override
    protected void tearDown() {
        super.tearDown();
        dirPath = null;
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return dirPath;
    }

    @NotNull
    @Override
    protected String fileName() {
        return getTestName(true) + ".kt";
    }
}
