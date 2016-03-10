/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.CLASS_ATTRIBUTE_SET;
import static com.android.SdkConstants.CLASS_CONTEXT;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;

import java.util.Collections;
import java.util.List;

import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.kinds.UastClassKind;

/**
 * Looks for custom views that do not define the view constructors needed by UI builders
 */
public class ViewConstructorDetector extends Detector implements UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ViewConstructor", //$NON-NLS-1$
            "Missing View constructors for XML inflation",

            "Some layout tools (such as the Android layout editor for Studio & Eclipse) needs to " +
            "find a constructor with one of the following signatures:\n" +
            "* `View(Context context)`\n" +
            "* `View(Context context, AttributeSet attrs)`\n" +
            "* `View(Context context, AttributeSet attrs, int defStyle)`\n" +
            "\n" +
            "If your custom view needs to perform initialization which does not apply when " +
            "used in a layout editor, you can surround the given code with a check to " +
            "see if `View#isInEditMode()` is false, since that method will return `false` " +
            "at runtime but true within a user interface editor.",

            Category.USABILITY,
            3,
            Severity.WARNING,
            new Implementation(
                    ViewConstructorDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link ViewConstructorDetector} check */
    public ViewConstructorDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements UastScanner ----

    private static boolean isXmlConstructor(UFunction method) {
        // Accept
        //   android.content.Context
        //   android.content.Context,android.util.AttributeSet
        //   android.content.Context,android.util.AttributeSet,int
        List<UVariable> valueParameters = method.getValueParameters();
        int valueParameterCount = valueParameters.size();
        if (valueParameterCount == 0 || valueParameterCount > 3) {
            return false;
        }

        if (!valueParameters.get(0).getType().matchesFqName(CLASS_CONTEXT)) {
            return false;
        }
        if (valueParameterCount == 1) {
            return true;
        }
        if (!valueParameters.get(1).getType().matchesFqName(CLASS_ATTRIBUTE_SET)) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (valueParameterCount == 2) {
            return true;
        }
        return valueParameters.get(2).getType().isInt();
    }

    @Nullable
    @Override
    public List<String> getApplicableSuperClasses() {
        return Collections.singletonList(SdkConstants.CLASS_VIEW);
    }

    @Override
    public void visitClass(UastAndroidContext context, UClass node) {
        // Only applies to concrete and not abstract classes
        UastClassKind kind = node.getKind();
        if (kind != UastClassKind.CLASS || node.hasModifier(UastModifier.ABSTRACT)) {
            return;
        }

        if (UastUtils.getContainingClass(node) != null && !node.hasModifier(UastModifier.STATIC)) {
            // Ignore inner classes that aren't static: we can't create these
            // anyway since we'd need the outer instance
            return;
        }

        boolean found = false;
        for (UFunction constructor : node.getConstructors()) {
            if (isXmlConstructor(constructor)) {
                found = true;
                break;
            }
        }

        if (!found) {
            String message = String.format(
              "Custom view `%1$s` is missing constructor used by tools: "
              + "`(Context)` or `(Context,AttributeSet)` "
              + "or `(Context,AttributeSet,int)`",
              node.getFqName());
            Location location = UastAndroidUtils.getLocation(node.getNameElement());
            context.report(ISSUE, node, location, message  /*data*/);
        }
    }
}
