/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.diagnostics;

import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.PositioningStrategies;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;

import static org.jetbrains.kotlin.diagnostics.PositioningStrategies.DECLARATION_RETURN_TYPE;
import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;

public interface ErrorsAssignmentPlugin {

    DiagnosticFactory0<KtDeclaration> DECLARATION_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT =
            DiagnosticFactory0.create(Severity.ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory0<KtSimpleNameExpression> CALL_ERROR_ASSIGN_METHOD_SHOULD_RETURN_UNIT =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.CALL_EXPRESSION);
    DiagnosticFactory0<KtSimpleNameExpression> NO_APPLICABLE_ASSIGN_METHOD =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.CALL_EXPRESSION);

    @SuppressWarnings("unused")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
                    ErrorsAssignmentPlugin.class,
                    DefaultErrorMessagesAssignmentPlugin.INSTANCE
            );
        }
    };
}
