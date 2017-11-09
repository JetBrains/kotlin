/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        myFixture.configureByFile(path);
        checkGotoDirectives(new GotoSymbolModel2(getProject()), myFixture.getEditor());
    }

    protected void doClassTest(String path) {
        myFixture.configureByFile(path);
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
