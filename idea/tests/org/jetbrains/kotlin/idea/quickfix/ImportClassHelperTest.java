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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;

import java.io.IOException;

public class ImportClassHelperTest extends LightDaemonAnalyzerTestCase {
    public void testDoNotImportIfGeneralExist() {
        doFileTest("jettesting.data.testFunction");
    }

    public void testDoNotImportIfGeneralSpaceExist() {
        doFileTest("jettesting.data.testFunction");
    }

    public void testNoDefaultImport() {
        doFileTest("kotlin.io.println");
    }

    public void testImportBeforeObject() {
        doFileTest("java.util.HashSet");
    }

    public void testInsertInEmptyFile() throws IOException {
        configureFromFileText("testInsertInEmptyFile.kt", "");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ImportInsertHelper.OBJECT$.getINSTANCE().addImportDirectiveIfNeeded(new FqName("java.util.ArrayList"), (JetFile) getFile());
            }
        });

        checkResultByText("import java.util.ArrayList");
    }

    public void testInsertInPackageOnlyFile() throws IOException {
        configureFromFileText("testInsertInPackageOnlyFile.kt", "package some");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ImportInsertHelper.OBJECT$.getINSTANCE().addImportDirectiveIfNeeded(new FqName("java.util.ArrayList"), (JetFile) getFile());
            }
        });

        checkResultByText("package some\n\nimport java.util.ArrayList");
    }

    public void doFileTest(final String importString) {
        configureByFile(getTestName(false) + ".kt");
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ImportInsertHelper.OBJECT$.getINSTANCE().addImportDirectiveIfNeeded(new FqName(importString), (JetFile) getFile());
            }
        });
        checkResultByFile(getTestName(false) + ".kt.after");
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/quickfix/importHelper/";
    }
}
