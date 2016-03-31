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

import static com.android.SdkConstants.ATTR_NAME;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;

import java.util.Collection;
import java.util.Collections;

/**
 * Checks that byte order marks do not appear in resource names
 */
public class ByteOrderMarkDetector extends ResourceXmlDetector {

    /** Detects BOM characters in the middle of files */
    public static final Issue BOM = Issue.create(
            "ByteOrderMark", //$NON-NLS-1$
            "Byte order mark inside files",
            "Lint will flag any byte-order-mark (BOM) characters it finds in the middle " +
            "of a file. Since we expect files to be encoded with UTF-8 (see the EnforceUTF8 " +
            "issue), the BOM characters are not necessary, and they are not handled correctly " +
            "by all tools. For example, if you have a BOM as part of a resource name in one " +
            "particular translation, that name will not be considered identical to the base " +
            "resource's name and the translation will not be used.",
            Category.I18N,
            8,
            Severity.FATAL,
            new Implementation(
                    ByteOrderMarkDetector.class,
                    Scope.RESOURCE_FILE_SCOPE))
            .addMoreInfo("http://en.wikipedia.org/wiki/Byte_order_mark");

    /** Constructs a new {@link ByteOrderMarkDetector} */
    public ByteOrderMarkDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Nullable
    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_NAME);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String name = attribute.getValue();
        for (int i = 0, n = name.length(); i < n; i++) {
            char c = name.charAt(i);
            if (c == '\uFEFF') {
                Location location = context.getLocation(attribute);
                String message = "Found byte-order-mark in the middle of a file";
                context.report(BOM, null, location, message);
                break;
            }
        }
    }
}
