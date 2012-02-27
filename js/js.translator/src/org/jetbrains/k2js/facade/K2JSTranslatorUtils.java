/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.facade;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.config.TestConfig;

/**
 * Created by IntelliJ IDEA.
 * User: Natalia.Ukhorskaya
 * Date: 2/9/12
 * Time: 7:49 PM
 */

public class K2JSTranslatorUtils {
    @SuppressWarnings("FieldCanBeLocal")
    private static String EXCEPTION = "exception=";

    @Nullable
    public String translateToJS(@NotNull Project project, @NotNull String code, @NotNull String arguments) {
        try {
            return generateJSCode(project, code, arguments);
        } catch (AssertionError e) {
            reportException(e);
            return EXCEPTION + "Translation error.";
        } catch (UnsupportedOperationException e) {
            reportException(e);
            return EXCEPTION + "Unsupported feature.";
        } catch (Throwable e) {
            reportException(e);
            return EXCEPTION + "Unexpected exception.";
        }
    }

    @Nullable
    public BindingContext getBindingContext(@NotNull Project project, @NotNull String programText) {
        try {
            K2JSTranslator k2JSTranslator = new K2JSTranslator(new TestConfig(project));
            return k2JSTranslator.analyzeProgramCode(programText);
        } catch (Throwable e) {
            e.printStackTrace();
            reportException(e);
            return null;
        }
    }

    @NotNull
    private String generateJSCode(@NotNull Project project, @NotNull String code, @NotNull String arguments) {
        String generatedCode = (new K2JSTranslator(new TestConfig(project))).translateStringWithCallToMain(code, arguments);
        return generatedCode;
    }

    private void reportException(@NotNull Throwable e) {
        System.out.println("Exception in translateToJS!!!");
        e.printStackTrace();
    }
}
