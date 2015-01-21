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

package org.jetbrains.kotlin.idea.imports;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.kotlin.idea.quickfix.ImportInsertHelper;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;

public class OptimizeImportsOnFlyTest extends LightDaemonAnalyzerTestCase {

    public void testOptimizeImportsOnFly() throws Exception {
        configureFromFileText("test.kt", "import java.util.ArrayList");
        boolean oldValue = CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY;
        try {
            CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY = true;
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    ImportInsertHelper.OBJECT$.getINSTANCE().addImportDirectiveIfNeeded(new FqName("java.util.HashSet"), (JetFile) getFile());
                }
            });
        }
        finally {
            CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY = oldValue;
        }
        checkResultByText("import java.util.HashSet");
    }
}
