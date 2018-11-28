/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Checks for errors related to Date Formats
 */
public class DateFormatDetector extends Detector implements Detector.UastScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            DateFormatDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Constructing SimpleDateFormat without an explicit locale */
    public static final Issue DATE_FORMAT = Issue.create(
            "SimpleDateFormat", //$NON-NLS-1$
            "Implied locale in date format",

            "Almost all callers should use `getDateInstance()`, `getDateTimeInstance()`, or " +
            "`getTimeInstance()` to get a ready-made instance of SimpleDateFormat suitable " +
            "for the user's locale. The main reason you'd create an instance this class " +
            "directly is because you need to format/parse a specific machine-readable format, " +
            "in which case you almost certainly want to explicitly ask for US to ensure that " +
            "you get ASCII digits (rather than, say, Arabic digits).\n" +
            "\n" +
            "Therefore, you should either use the form of the SimpleDateFormat constructor " +
            "where you pass in an explicit locale, such as Locale.US, or use one of the " +
            "get instance methods, or suppress this error if really know what you are doing.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo(
            "http://developer.android.com/reference/java/text/SimpleDateFormat.html");//$NON-NLS-1$

    public static final String LOCALE_CLS = "java.util.Locale";                       //$NON-NLS-1$
    public static final String SIMPLE_DATE_FORMAT_CLS = "java.text.SimpleDateFormat"; //$NON-NLS-1$

    /** Constructs a new {@link DateFormatDetector} */
    public DateFormatDetector() {
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> getApplicableConstructorTypes() {
        return Collections.singletonList(SIMPLE_DATE_FORMAT_CLS);
    }

    @Override
    public void visitConstructor(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression node, @NonNull UMethod constructor) {
        if (!specifiesLocale(constructor)) {
            Location location = context.getUastLocation(node);
            String message =
                    "To get local formatting use `getDateInstance()`, `getDateTimeInstance()`, " +
                            "or `getTimeInstance()`, or use `new SimpleDateFormat(String template, " +
                            "Locale locale)` with for example `Locale.US` for ASCII dates.";
            context.report(DATE_FORMAT, node, location, message);
        }
    }

    private static boolean specifiesLocale(@NonNull PsiMethod method) {
        PsiParameterList parameterList = method.getParameterList();
        PsiParameter[] parameters = parameterList.getParameters();
        for (PsiParameter parameter : parameters) {
            PsiType type = parameter.getType();
            if (type.getCanonicalText().equals(LOCALE_CLS)) {
                    return true;
            }
        }

        return false;
    }
}
