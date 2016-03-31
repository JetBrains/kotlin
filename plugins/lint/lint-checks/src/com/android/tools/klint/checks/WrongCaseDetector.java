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

package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;

/**
 * Check which looks for missing wrong case usage for certain layout tags.
 *
 * TODO: Generalize this to handling spelling errors in general.
 */
public class WrongCaseDetector extends LayoutDetector {
    /** Using the wrong case for layout tags */
    public static final Issue WRONG_CASE = Issue.create(
            "WrongCase", //$NON-NLS-1$
            "Wrong case for view tag",

            "Most layout tags, such as <Button>, refer to actual view classes and are therefore " +
            "capitalized. However, there are exceptions such as <fragment> and <include>. This " +
            "lint check looks for incorrect capitalizations.",

            Category.CORRECTNESS,
            4,
            Severity.FATAL,
            new Implementation(
                    WrongCaseDetector.class,
                    Scope.RESOURCE_FILE_SCOPE))
            .addMoreInfo("http://developer.android.com/guide/components/fragments.html"); //$NON-NLS-1$

    /** Constructs a new {@link WrongCaseDetector} */
    public WrongCaseDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                "Fragment",      //$NON-NLS-1$
                "RequestFocus",  //$NON-NLS-1$
                "Include",       //$NON-NLS-1$
                "Merge"
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tag = element.getTagName();
        String correct = Character.toLowerCase(tag.charAt(0)) + tag.substring(1);
        context.report(WRONG_CASE, element, context.getLocation(element),
                String.format("Invalid tag `<%1$s>`; should be `<%2$s>`", tag, correct));
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * returns the old value to be replaced in the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding old value, or null if not recognized
     */
    @Nullable
    public static String getOldValue(@NonNull String errorMessage, @NonNull TextFormat format) {
        return LintUtils.findSubstring(format.toText(errorMessage), " tag <", ">");
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * returns the new value to be put into the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding new value, or null if not recognized
     */
    @Nullable
    public static String getNewValue(@NonNull String errorMessage, @NonNull TextFormat format) {
        return LintUtils.findSubstring(format.toText(errorMessage), " should be <", ">");
    }
}
