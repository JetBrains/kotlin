/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.SdkConstants.CLASS_FRAGMENT;
import static com.android.SdkConstants.CLASS_VIEW;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;

import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Looks for leaks via static fields
 */
public class LeakDetector extends Detector implements Detector.UastScanner {
    /** Leaking data via static fields */
    public static final Issue ISSUE = Issue.create(
            "StaticFieldLeak", //$NON-NLS-1$
            "Static Field Leaks",

            "A static field will leak contexts.",

            Category.PERFORMANCE,
            6,
            Severity.WARNING,
            new Implementation(
                    LeakDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link LeakDetector} check */
    public LeakDetector() {
    }

    // ---- Implements JavaScanner ----

    @Override
    public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
        return Collections.<Class<? extends PsiElement>>singletonList(PsiField.class);
    }

    @Nullable
    @Override
    public UastVisitor createUastVisitor(@NonNull JavaContext context) {
        return new FieldChecker(context);
    }

    private static class FieldChecker extends AbstractUastVisitor {
        private final JavaContext mContext;

        private FieldChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitClass(@NotNull UClass node) {
            return super.visitClass(node);
        }

        @Override
        public boolean visitVariable(UVariable node) {
            if (node instanceof UField) {
                checkField((UField) node);
            }
            return super.visitVariable(node);
        }

        private void checkField(UField field) {
            PsiModifierList modifierList = field.getModifierList();
            if (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                return;
            }

            PsiType type = field.getType();
            if (!(type instanceof PsiClassType)) {
                return;
            }

            String fqn = type.getCanonicalText();
            if (fqn.startsWith("java.")) {
                return;
            }
            PsiClass cls = ((PsiClassType) type).resolve();
            if (cls == null) {
                return;
            }
            if (fqn.startsWith("android.")) {
                if (isLeakCandidate(cls)) {
                    String message = "Do not place Android context classes in static fields; "
                            + "this is a memory leak (and also breaks Instant Run)";
                    report(field, message);
                }
            } else {
                // User application object -- look to see if that one itself has
                // static fields?
                // We only check *one* level of indirection here
                int count = 0;
                for (PsiField referenced : cls.getAllFields()) {
                    // Only check a few; avoid getting bogged down on large classes
                    if (count++ == 20) {
                        break;
                    }

                    PsiType innerType = referenced.getType();
                    if (!(innerType instanceof PsiClassType)) {
                        continue;
                    }

                    fqn = innerType.getCanonicalText();
                    if (fqn.startsWith("java.")) {
                        continue;
                    }
                    PsiClass innerCls = ((PsiClassType) innerType).resolve();
                    if (innerCls == null) {
                        continue;
                    }
                    if (fqn.startsWith("android.")) {
                        if (isLeakCandidate(innerCls)) {
                            String message =
                                    "Do not place Android context classes in static fields "
                                            + "(static reference to `"
                                            + cls.getName() + "` which has field "
                                            + "`" + referenced.getName() + "` pointing to `"
                                            + innerCls.getName() + "`); "
                                        + "this is a memory leak (and also breaks Instant Run)";
                            report(field, message);
                            break;
                        }
                    }
                }
            }
        }

        private void report(@NonNull UElement element, @NonNull String message) {
            mContext.report(ISSUE, element, mContext.getUastLocation(element), message);
        }
    }

    private static boolean isLeakCandidate(@NonNull PsiClass cls) {
        return InheritanceUtil.isInheritor(cls, false, CLASS_CONTEXT)
                               || InheritanceUtil.isInheritor(cls, false, CLASS_VIEW)
                               || InheritanceUtil.isInheritor(cls, false, CLASS_FRAGMENT);
    }
}
