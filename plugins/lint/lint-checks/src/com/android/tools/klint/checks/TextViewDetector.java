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
import static com.android.SdkConstants.ATTR_AUTO_TEXT;
import static com.android.SdkConstants.ATTR_BUFFER_TYPE;
import static com.android.SdkConstants.ATTR_CAPITALIZE;
import static com.android.SdkConstants.ATTR_CURSOR_VISIBLE;
import static com.android.SdkConstants.ATTR_DIGITS;
import static com.android.SdkConstants.ATTR_EDITABLE;
import static com.android.SdkConstants.ATTR_EDITOR_EXTRAS;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_IME_ACTION_ID;
import static com.android.SdkConstants.ATTR_IME_ACTION_LABEL;
import static com.android.SdkConstants.ATTR_IME_OPTIONS;
import static com.android.SdkConstants.ATTR_INPUT_METHOD;
import static com.android.SdkConstants.ATTR_INPUT_TYPE;
import static com.android.SdkConstants.ATTR_NUMERIC;
import static com.android.SdkConstants.ATTR_ON_CLICK;
import static com.android.SdkConstants.ATTR_PASSWORD;
import static com.android.SdkConstants.ATTR_PHONE_NUMBER;
import static com.android.SdkConstants.ATTR_PRIVATE_IME_OPTIONS;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.ATTR_TEXT_IS_SELECTABLE;
import static com.android.SdkConstants.ATTR_VISIBILITY;
import static com.android.SdkConstants.BUTTON;
import static com.android.SdkConstants.CHECKED_TEXT_VIEW;
import static com.android.SdkConstants.CHECK_BOX;
import static com.android.SdkConstants.RADIO_BUTTON;
import static com.android.SdkConstants.SWITCH;
import static com.android.SdkConstants.TEXT_VIEW;
import static com.android.SdkConstants.TOGGLE_BUTTON;
import static com.android.SdkConstants.VALUE_EDITABLE;
import static com.android.SdkConstants.VALUE_NONE;
import static com.android.SdkConstants.VALUE_TRUE;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.Arrays;
import java.util.Collection;

/**
 * Checks for cases where a TextView should probably be an EditText instead
 */
public class TextViewDetector extends LayoutDetector {

    private static final Implementation IMPLEMENTATION = new Implementation(
            TextViewDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "TextViewEdits", //$NON-NLS-1$
            "TextView should probably be an EditText instead",

            "Using a `<TextView>` to input text is generally an error, you should be " +
            "using `<EditText>` instead.  `EditText` is a subclass of `TextView`, and some " +
            "of the editing support is provided by `TextView`, so it's possible to set " +
            "some input-related properties on a `TextView`. However, using a `TextView` " +
            "along with input attributes is usually a cut & paste error. To input " +
            "text you should be using `<EditText>`.\n" +
            "\n" +
            "This check also checks subclasses of `TextView`, such as `Button` and `CheckBox`, " +
            "since these have the same issue: they should not be used with editable " +
            "attributes.",

            Category.CORRECTNESS,
            7,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Text could be selectable */
    public static final Issue SELECTABLE = Issue.create(
            "SelectableText", //$NON-NLS-1$
            "Dynamic text should probably be selectable",

            "If a `<TextView>` is used to display data, the user might want to copy that " +
            "data and paste it elsewhere. To allow this, the `<TextView>` should specify " +
            "`android:textIsSelectable=\"true\"`.\n" +
            "\n" +
            "This lint check looks for TextViews which are likely to be displaying data: " +
            "views whose text is set dynamically. This value will be ignored on platforms " +
            "older than API 11, so it is okay to set it regardless of your `minSdkVersion`.",

            Category.USABILITY,
            7,
            Severity.WARNING,
            IMPLEMENTATION)
            // Apparently setting this can have some undesirable side effects
            .setEnabledByDefault(false);

    /** Constructs a new {@link TextViewDetector} */
    public TextViewDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TEXT_VIEW,
                BUTTON,
                TOGGLE_BUTTON,
                CHECK_BOX,
                RADIO_BUTTON,
                CHECKED_TEXT_VIEW,
                SWITCH
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (element.getTagName().equals(TEXT_VIEW)) {
            if (!element.hasAttributeNS(ANDROID_URI, ATTR_TEXT)
                    && element.hasAttributeNS(ANDROID_URI, ATTR_ID)
                    && !element.hasAttributeNS(ANDROID_URI, ATTR_TEXT_IS_SELECTABLE)
                    && !element.hasAttributeNS(ANDROID_URI, ATTR_VISIBILITY)
                    && !element.hasAttributeNS(ANDROID_URI, ATTR_ON_CLICK)
                    && context.getMainProject().getTargetSdk() >= 11
                    && context.isEnabled(SELECTABLE)) {
                context.report(SELECTABLE, element, context.getLocation(element),
                        "Consider making the text value selectable by specifying " +
                        "`android:textIsSelectable=\"true\"`");
            }
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String name = attribute.getLocalName();
            if (name == null || name.isEmpty()) {
                // Attribute not in a namespace; we only care about the android: ones
                continue;
            }

            boolean isEditAttribute = false;
            switch (name.charAt(0)) {
                case 'a': {
                    isEditAttribute = name.equals(ATTR_AUTO_TEXT);
                    break;
                }
                case 'b': {
                    isEditAttribute = name.equals(ATTR_BUFFER_TYPE) &&
                            attribute.getValue().equals(VALUE_EDITABLE);
                    break;
                }
                case 'p': {
                    isEditAttribute = name.equals(ATTR_PASSWORD)
                            || name.equals(ATTR_PHONE_NUMBER)
                            || name.equals(ATTR_PRIVATE_IME_OPTIONS);
                    break;
                }
                case 'c': {
                    isEditAttribute = name.equals(ATTR_CAPITALIZE)
                            || name.equals(ATTR_CURSOR_VISIBLE);
                    break;
                }
                case 'd': {
                    isEditAttribute = name.equals(ATTR_DIGITS);
                    break;
                }
                case 'e': {
                    if (name.equals(ATTR_EDITABLE)) {
                        isEditAttribute = attribute.getValue().equals(VALUE_TRUE);
                    } else {
                        isEditAttribute = name.equals(ATTR_EDITOR_EXTRAS);
                    }
                    break;
                }
                case 'i': {
                    if (name.equals(ATTR_INPUT_TYPE)) {
                        String value = attribute.getValue();
                        isEditAttribute = !value.isEmpty() && !value.equals(VALUE_NONE);
                    } else {
                        isEditAttribute = name.equals(ATTR_INPUT_TYPE)
                                || name.equals(ATTR_IME_OPTIONS)
                                || name.equals(ATTR_IME_ACTION_LABEL)
                                || name.equals(ATTR_IME_ACTION_ID)
                                || name.equals(ATTR_INPUT_METHOD);
                    }
                    break;
                }
                case 'n': {
                    isEditAttribute = name.equals(ATTR_NUMERIC);
                    break;
                }
            }

            if (isEditAttribute && ANDROID_URI.equals(attribute.getNamespaceURI()) && context.isEnabled(ISSUE)) {
                Location location = context.getLocation(attribute);
                String message;
                String view = element.getTagName();
                if (view.equals(TEXT_VIEW)) {
                    message = String.format(
                            "Attribute `%1$s` should not be used with `<TextView>`: " +
                            "Change element type to `<EditText>` ?", attribute.getName());
                } else {
                    message = String.format(
                            "Attribute `%1$s` should not be used with `<%2$s>`: " +
                            "intended for editable text widgets",
                            attribute.getName(), view);
                }
                context.report(ISSUE, attribute, location, message);
            }
        }
    }
}
