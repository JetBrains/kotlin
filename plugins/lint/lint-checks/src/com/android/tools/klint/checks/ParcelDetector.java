/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.SdkConstants.CLASS_PARCELABLE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;

import com.intellij.psi.util.InheritanceUtil;
import kotlinx.android.parcel.Parcelize;
import org.jetbrains.uast.UAnonymousClass;
import org.jetbrains.uast.UClass;

import java.util.Collections;
import java.util.List;

/**
 * Looks for Parcelable classes that are missing a CREATOR field
 */
public class ParcelDetector extends Detector implements Detector.UastScanner {

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ParcelCreator", //$NON-NLS-1$
            "Missing Parcelable `CREATOR` field",

            "According to the `Parcelable` interface documentation, " +
            "\"Classes implementing the Parcelable interface must also have a " +
            "static field called `CREATOR`, which is an object implementing the " +
            "`Parcelable.Creator` interface.\"",

            Category.CORRECTNESS,
            3,
            Severity.ERROR,
            new Implementation(
                    ParcelDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo("http://developer.android.com/reference/android/os/Parcelable.html");

    /** Constructs a new {@link ParcelDetector} check */
    public ParcelDetector() {
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(CLASS_PARCELABLE);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        if (declaration instanceof UAnonymousClass) {
            // Anonymous classes aren't parcelable
            return;
        }

        // Only applies to concrete classes
        if (declaration.isInterface()) {
            return;
        }
        if (declaration.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return;
        }

        // Parceling spans is handled in TextUtils#CHAR_SEQUENCE_CREATOR
        if (InheritanceUtil.isInheritor(declaration, false, "android.text.ParcelableSpan")) {
            return;
        }

        // Do not report errors on our Android Extensions-improved Parcelables
        if (declaration.findAnnotation(Parcelize.class.getName()) != null) {
            return;
        }

        PsiField field = declaration.findFieldByName("CREATOR", false);
        if (field == null) {
            Location location = context.getUastNameLocation(declaration);
            context.reportUast(ISSUE, declaration, location,
                    "This class implements `Parcelable` but does not "
                            + "provide a `CREATOR` field");
        }
    }
}
