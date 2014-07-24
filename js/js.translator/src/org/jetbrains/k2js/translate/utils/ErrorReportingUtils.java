/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetExpression;

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
    public static String message(@NotNull DeclarationDescriptor descriptor, @NotNull String explainingMessage) {
        return explainingMessage + " at " + DiagnosticUtils.atLocation(descriptor) + ".";
    }

    @NotNull
    public static String message(@NotNull PsiElement element) {
        return "Error at " + DiagnosticUtils.atLocation(element) + ".";
    }
}
