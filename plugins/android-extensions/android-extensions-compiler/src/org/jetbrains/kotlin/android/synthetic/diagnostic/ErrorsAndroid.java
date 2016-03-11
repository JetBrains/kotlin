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

package org.jetbrains.kotlin.android.synthetic.diagnostic;

import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtExpression;

import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface ErrorsAndroid {
    DiagnosticFactory1<KtExpression, String> SYNTHETIC_INVALID_WIDGET_TYPE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<KtExpression, String> SYNTHETIC_UNRESOLVED_WIDGET_TYPE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtExpression> SYNTHETIC_DEPRECATED_PACKAGE = DiagnosticFactory0.create(WARNING);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsAndroid.class);
        }
    };

}
