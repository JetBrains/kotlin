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

import static com.android.SdkConstants.TAG_RESOURCES;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Looks for problems with XML files being placed in the wrong folder */
public class WrongLocationDetector extends LayoutDetector {
    /** Main issue investigated by this detector */
    public static final Issue ISSUE = Issue.create(
            "WrongFolder", //$NON-NLS-1$
            "Resource file in the wrong `res` folder",

            "Resource files are sometimes placed in the wrong folder, and it can lead to " +
            "subtle bugs that are hard to understand. This check looks for problems in this " +
            "area, such as attempting to place a layout \"alias\" file in a `layout/` folder " +
            "rather than the `values/` folder where it belongs.",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            new Implementation(
                    WrongLocationDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Constructs a new {@link WrongLocationDetector} check */
    public WrongLocationDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        Element root = document.getDocumentElement();
        if (root != null && root.getTagName().equals(TAG_RESOURCES)) {
            context.report(ISSUE, root, context.getLocation(root),
                    "This file should be placed in a `values`/ folder, not a `layout`/ folder");
        }
    }
}
