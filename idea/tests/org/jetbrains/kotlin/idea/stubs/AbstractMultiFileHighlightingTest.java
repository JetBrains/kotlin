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

package org.jetbrains.kotlin.idea.stubs;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.AstAccessControl;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;

import java.io.File;

public abstract class AbstractMultiFileHighlightingTest extends AbstractMultiHighlightingTest {

    public void doTest(@NotNull String filePath) throws Exception {
        configureByFile(new File(filePath).getName(), "");
        boolean shouldFail = getName().contains("UnspecifiedType");
        AstAccessControl.INSTANCE.testWithControlledAccessToAst(
                shouldFail, getFile().getVirtualFile(), getProject(), getTestRootDisposable(),
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        checkHighlighting(myEditor, true, false);
                        return Unit.INSTANCE;
                    }
                }
        );
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/multiFileHighlighting/";
    }
}
