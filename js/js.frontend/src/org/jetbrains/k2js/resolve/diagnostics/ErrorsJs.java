/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.resolve.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.PositioningStrategies.CALL_EXPRESSION;
import static org.jetbrains.jet.lang.diagnostics.PositioningStrategies.DECLARATION_RETURN_TYPE;
import static org.jetbrains.jet.lang.diagnostics.PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT;
import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;
import static org.jetbrains.jet.lang.diagnostics.Severity.WARNING;

public interface ErrorsJs {
    DiagnosticFactory1<JetElement, JetType> NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<JetElement, String> NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<JetElement, String> NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<JetDeclaration> NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE = DiagnosticFactory0.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory0<JetDeclaration> NATIVE_SETTER_WRONG_RETURN_TYPE = DiagnosticFactory0.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<JetElement, Integer, String> NATIVE_INDEXER_WRONG_PARAMETER_COUNT = DiagnosticFactory2.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory2<JetExpression, String, List<TextRange>> JSCODE_ERROR = DiagnosticFactory2.create(ERROR, JsCodePositioningStrategy.INSTANCE$);
    DiagnosticFactory2<JetExpression, String, List<TextRange>> JSCODE_WARNING = DiagnosticFactory2.create(WARNING, JsCodePositioningStrategy.INSTANCE$);
    DiagnosticFactory0<JetExpression> JSCODE_ARGUMENT_SHOULD_BE_LITERAL = DiagnosticFactory0.create(ERROR, CALL_EXPRESSION);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJs.class);
        }
    };
}
