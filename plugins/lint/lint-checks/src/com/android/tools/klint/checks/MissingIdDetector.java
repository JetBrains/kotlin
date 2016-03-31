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
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_TAG;
import static com.android.SdkConstants.VIEW_FRAGMENT;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;

/**
 * Check which looks for missing id's in views where they are probably needed
 */
public class MissingIdDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "MissingId", //$NON-NLS-1$
            "Fragments should specify an `id` or `tag`",

            "If you do not specify an android:id or an android:tag attribute on a " +
            "<fragment> element, then if the activity is restarted (for example for " +
            "an orientation rotation) you may lose state. From the fragment " +
            "documentation:\n" +
            "\n" +
            "\"Each fragment requires a unique identifier that the system can use " +
            "to restore the fragment if the activity is restarted (and which you can " +
            "use to capture the fragment to perform transactions, such as remove it).\n" +
            "\n" +
            "* Supply the android:id attribute with a unique ID.\n" +
            "* Supply the android:tag attribute with a unique string.\n" +
            "If you provide neither of the previous two, the system uses the ID of the " +
            "container view.",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            new Implementation(
                    MissingIdDetector.class,
                    Scope.RESOURCE_FILE_SCOPE))
            .addMoreInfo("http://developer.android.com/guide/components/fragments.html"); //$NON-NLS-1$

    /** Constructs a new {@link MissingIdDetector} */
    public MissingIdDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(VIEW_FRAGMENT);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (!element.hasAttributeNS(ANDROID_URI, ATTR_ID) &&
                !element.hasAttributeNS(ANDROID_URI, ATTR_TAG)) {
            context.report(ISSUE, element, context.getLocation(element),
                "This `<fragment>` tag should specify an id or a tag to preserve state " +
                "across activity restarts");
        }
    }
}
