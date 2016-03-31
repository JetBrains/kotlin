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

package com.android.tools.lint.checks;


import static com.android.tools.lint.checks.PermissionRequirement.ATTR_PROTECTION_LEVEL;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;

import java.util.Collection;
import java.util.Collections;

/**
 * Checks if signatureOrSystem level permissions are set.
 */
public class SignatureOrSystemDetector extends Detector implements Detector.XmlScanner {
    public static final Issue ISSUE = Issue.create(
            "SignatureOrSystemPermissions", //$NON-NLS-1$
            "signatureOrSystem permissions declared",
            "The `signature` protection level should probably be sufficient for most needs and "
                    + "works regardless of where applications are installed. The "
                    + "`signatureOrSystem` level is used for certain situations where "
                    + "multiple vendors have applications built into a system image and "
                    + "need to share specific features explicitly because they are being built "
                    + "together.",
            Category.SECURITY,
            5,
            Severity.WARNING,
            new Implementation(
                    SignatureOrSystemDetector.class,
                    Scope.MANIFEST_SCOPE
            ));
    private static final String SIGNATURE_OR_SYSTEM = "signatureOrSystem"; //$NON-NLS-1$

    // ---- Implements Detector.XmlScanner ----

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_PROTECTION_LEVEL);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String protectionLevel = attribute.getValue();
        if (protectionLevel != null
            && protectionLevel.equals(SIGNATURE_OR_SYSTEM)) {
            String message = "`protectionLevel` should probably not be set to `signatureOrSystem`";
            context.report(ISSUE, attribute, context.getLocation(attribute), message);
        }
    }
}
