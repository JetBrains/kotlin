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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_HINT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LABEL_FOR;
import static com.android.SdkConstants.AUTO_COMPLETE_TEXT_VIEW;
import static com.android.SdkConstants.EDIT_TEXT;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.MULTI_AUTO_COMPLETE_TEXT_VIEW;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.tools.lint.detector.api.LintUtils.stripIdPrefix;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.Sets;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Detector which finds unlabeled text fields
 */
public class LabelForDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "LabelFor", //$NON-NLS-1$
            "Missing `labelFor` attribute",

            "Text fields should be labelled with a `labelFor` attribute, " +
            "provided your `minSdkVersion` is at least 17.\n" +
            "\n" +
            "If your view is labeled but by a label in a different layout which " +
            "includes this one, just suppress this warning from lint.",
            Category.A11Y,
            2,
            Severity.WARNING,
            new Implementation(
                    LabelForDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    private Set<String> mLabels;
    private List<Element> mTextFields;

    /** Constructs a new {@link LabelForDetector} */
    public LabelForDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    @Nullable
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_LABEL_FOR);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                EDIT_TEXT,
                AUTO_COMPLETE_TEXT_VIEW,
                MULTI_AUTO_COMPLETE_TEXT_VIEW
        );
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (mTextFields != null) {
            if (mLabels == null) {
                mLabels = Collections.emptySet();
            }

            for (Element element : mTextFields) {
                if (element.hasAttributeNS(ANDROID_URI, ATTR_HINT)) {
                    continue;
                }
                String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);
                boolean missing = true;
                if (mLabels.contains(id)) {
                    missing = false;
                } else if (id.startsWith(NEW_ID_PREFIX)) {
                    missing = !mLabels.contains(ID_PREFIX + stripIdPrefix(id));
                } else if (id.startsWith(ID_PREFIX)) {
                    missing = !mLabels.contains(NEW_ID_PREFIX + stripIdPrefix(id));
                }

                if (missing) {
                    XmlContext xmlContext = (XmlContext) context;
                    Location location = xmlContext.getLocation(element);
                    String message;
                    if (id == null || id.isEmpty()) {
                        message = "No label views point to this text field with a " +
                                "`labelFor` attribute";
                    } else {
                        message = String.format("No label views point to this text field with " +
                                "an `android:labelFor=\"@+id/%1$s\"` attribute", id);
                    }
                    xmlContext.report(ISSUE, element, location, message);
                }

            }
        }

        mLabels = null;
        mTextFields = null;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        if (mLabels == null) {
            mLabels = Sets.newHashSet();
        }
        mLabels.add(attribute.getValue());
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // NOTE: This should NOT be checking *minSdkVersion*, but *targetSdkVersion*
        // or even buildTarget instead. However, there's a risk that this will flag
        // way too much and make the rule annoying until API 17 support becomes
        // more widespread, so for now limit the check to those projects *really*
        // working with 17.  When API 17 reaches a certain amount of adoption, change
        // this to flag all apps supporting 17, including those supporting earlier
        // versions as well.
        if (context.getMainProject().getMinSdk() < 17) {
            return;
        }

        if (mTextFields == null) {
            mTextFields = new ArrayList<Element>();
        }
        mTextFields.add(element);
    }
}
