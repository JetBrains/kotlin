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

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that the encoding used in resource files is always UTF-8
 * <p>
 * TODO: Add a check which looks at files which do not specify the encoding
 * and check the contents to see if it contains characters where it's ambiguous.
 */
public class Utf8Detector extends ResourceXmlDetector {

    /** Detects non-utf8 encodings */
    public static final Issue ISSUE = Issue.create(
            "EnforceUTF8", //$NON-NLS-1$
            "Encoding used in resource files is not UTF-8",
            "XML supports encoding in a wide variety of character sets. However, not all " +
            "tools handle the XML encoding attribute correctly, and nearly all Android " +
            "apps use UTF-8, so by using UTF-8 you can protect yourself against subtle " +
            "bugs when using non-ASCII characters.\n" +
            "\n" +
            "In particular, the Android Gradle build system will merge resource XML files " +
            "assuming the resource files are using UTF-8 encoding.\n",
            Category.I18N,
            5,
            Severity.FATAL,
            new Implementation(
                    Utf8Detector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** See http://www.w3.org/TR/REC-xml/#NT-EncodingDecl */
    private static final Pattern ENCODING_PATTERN =
            Pattern.compile("encoding=['\"](\\S*)['\"]");//$NON-NLS-1$

    /** Constructs a new {@link Utf8Detector} */
    public Utf8Detector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        String xml = context.getContents();
        if (xml == null) {
            return;
        }

        // AAPT: The prologue must be in the first line
        int lineEnd = 0;
        int max = xml.length();
        for (; lineEnd < max; lineEnd++) {
            char c = xml.charAt(lineEnd);
            if (c == '\n' || c == '\r') {
                break;
            }
        }

        for (int i = 16; i < lineEnd - 5; i++) { // +4: Skip at least <?xml encoding="
            if ((xml.charAt(i) == 'u' || xml.charAt(i) == 'U')
                    && (xml.charAt(i + 1) == 't' || xml.charAt(i + 1) == 'T')
                    && (xml.charAt(i + 2) == 'f' || xml.charAt(i + 2) == 'F')
                    && (xml.charAt(i + 3) == '-' || xml.charAt(i + 3) == '_')
                    && (xml.charAt(i + 4) == '8')) {
                return;
            }
        }

        int encodingIndex = xml.lastIndexOf("encoding", lineEnd); //$NON-NLS-1$
        if (encodingIndex != -1) {
            Matcher matcher = ENCODING_PATTERN.matcher(xml);
            if (matcher.find(encodingIndex)) {
                String encoding = matcher.group(1);
                Location location = Location.create(context.file, xml,
                        matcher.start(1), matcher.end(1));
                context.report(ISSUE, null, location, String.format(
                        "%1$s: Not using UTF-8 as the file encoding. This can lead to subtle " +
                                "bugs with non-ascii characters", encoding));
            }
        }
    }
}
