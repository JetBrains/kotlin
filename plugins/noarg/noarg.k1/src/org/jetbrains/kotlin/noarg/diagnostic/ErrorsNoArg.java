/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.noarg.diagnostic;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.Errors;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface ErrorsNoArg {
    DiagnosticFactory0<PsiElement> NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> NOARG_ON_INNER_CLASS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> NOARG_ON_INNER_CLASS_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NOARG_ON_LOCAL_CLASS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> NOARG_ON_LOCAL_CLASS_ERROR = DiagnosticFactory0.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(ErrorsNoArg.class, DefaultErrorMessagesNoArg.INSTANCE);
        }
    };
}
