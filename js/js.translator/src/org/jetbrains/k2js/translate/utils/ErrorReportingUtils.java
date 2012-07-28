/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.utils;

import com.google.common.collect.Lists;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class ErrorReportingUtils {
    private ErrorReportingUtils() {
    }

    @NotNull
    public static RuntimeException reportErrorWithLocation(@NotNull JetExpression selector, @NotNull RuntimeException e) {
        return reportErrorWithLocation(e, DiagnosticUtils.atLocation(selector));
    }

    @NotNull
    private static RuntimeException reportErrorWithLocation(@NotNull RuntimeException e, @NotNull String location) {
        throw new RuntimeException(e.getMessage() + " at " + location, e);
    }

    @NotNull
    public static String message(@NotNull PsiElement expression, @NotNull String messageText) {
        return messageText + " at " + DiagnosticUtils.atLocation(expression) + ".";
    }

    @NotNull
    public static String message(@NotNull BindingContext context,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull String explainingMessage) {
        return explainingMessage + " at " + DiagnosticUtils.atLocation(context, descriptor) + ".";
    }

    @NotNull
    public static String message(@NotNull PsiElement element) {
        return "Error at " + DiagnosticUtils.atLocation(element) + ".";
    }

    @NotNull
    public static RuntimeException reportErrorWithLocation(@NotNull RuntimeException e,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull BindingContext bindingContext) {
        throw reportErrorWithLocation(e, DiagnosticUtils.atLocation(bindingContext, descriptor));
    }

    @NotNull
    public static String atLocation(@Nullable JsExpression expression, @NotNull List<JsExpression> arguments) {
        List<JsExpression> list = Lists.newArrayList(expression);
        list.addAll(arguments);
        for (JsExpression value : arguments) {
            Source source = value.getSource();
            if (source != null) {
                return "at " + source + " " + value.getSourceLine() + ":" + value.getSourceColumn();
            }
        }
        return "at unknown location";
    }
}
