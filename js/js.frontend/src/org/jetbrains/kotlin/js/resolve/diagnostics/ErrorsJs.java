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

package org.jetbrains.kotlin.js.resolve.diagnostics;

import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.diagnostics.PositioningStrategies.*;
import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface ErrorsJs {
    DiagnosticFactory1<KtElement, KotlinType> NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtElement, String> NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtElement, String> NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtDeclaration> NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE = DiagnosticFactory0.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory0<KtDeclaration> NATIVE_SETTER_WRONG_RETURN_TYPE = DiagnosticFactory0.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<KtElement, Integer, String> NATIVE_INDEXER_WRONG_PARAMETER_COUNT = DiagnosticFactory2.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<KtExpression, JsCallData> JSCODE_ERROR = DiagnosticFactory1.create(ERROR, JsCodePositioningStrategy.INSTANCE);
    DiagnosticFactory1<KtExpression, JsCallData> JSCODE_WARNING = DiagnosticFactory1.create(WARNING, JsCodePositioningStrategy.INSTANCE);
    DiagnosticFactory0<KtExpression> JSCODE_ARGUMENT_SHOULD_BE_CONSTANT = DiagnosticFactory0.create(ERROR, DEFAULT);
    DiagnosticFactory1<KtElement, KtElement> NOT_SUPPORTED = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory1<KtElement, KtElement> REFERENCE_TO_BUILTIN_MEMBERS_NOT_SUPPORTED = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory1<KtNamedDeclaration, String> NON_TOPLEVEL_CLASS_DECLARATION = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<KtExpression> JSCODE_NO_JAVASCRIPT_PRODUCED = DiagnosticFactory0.create(ERROR, DEFAULT);
    DiagnosticFactory0<KtExpression> NATIVE_INNER_CLASS_PROHIBITED = DiagnosticFactory0.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJs.class);
        }
    };
}
