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
import com.android.ide.common.resources.configuration.LocaleQualifier;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.checks.PluralsDatabase.Quantity;
import com.android.tools.lint.detector.api.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import static com.android.SdkConstants.*;

/**
 * Checks for issues with quantity strings
 * <p>
 * https://code.google.com/p/android/issues/detail?id=53015
 * 53015: lint could report incorrect usage of Resource.getQuantityString
 */
public class PluralsDetector extends ResourceXmlDetector {
    private static final Implementation IMPLEMENTATION = new Implementation(
            PluralsDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** This locale should define a quantity string for the given quantity */
    public static final Issue MISSING = Issue.create(
            "MissingQuantity", //$NON-NLS-1$
            "Missing quantity translation",
            "Different languages have different rules for grammatical agreement with " +
            "quantity. In English, for example, the quantity 1 is a special case. " +
            "We write \"1 book\", but for any other quantity we'd write \"n books\". " +
            "This distinction between singular and plural is very common, but other " +
            "languages make finer distinctions.\n" +
            "\n" +
            "This lint check looks at each translation of a `<plural>` and makes sure " +
            "that all the quantity strings considered by the given language are provided " +
            "by this translation.\n" +
            "\n" +
            "For example, an English translation must provide a string for `quantity=\"one\"`. " +
            "Similarly, a Czech translation must provide a string for `quantity=\"few\"`.",
            Category.MESSAGES,
            8,
            Severity.ERROR,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/guide/topics/resources/string-resource.html#Plurals");

    /** This translation is not needed in this locale */
    public static final Issue EXTRA = Issue.create(
            "UnusedQuantity", //$NON-NLS-1$
            "Unused quantity translations",
            "Android defines a number of different quantity strings, such as `zero`, `one`, " +
            "`few` and `many`. However, many languages do not distinguish grammatically " +
            "between all these different quantities.\n" +
            "\n" +
            "This lint check looks at the quantity strings defined for each translation and " +
            "flags any quantity strings that are unused (because the language does not make that " +
            "quantity distinction, and Android will therefore not look it up.).\n" +
            "\n" +
            "For example, in Chinese, only the `other` quantity is used, so even if you " +
            "provide translations for `zero` and `one`, these strings will *not* be returned " +
            "when `getQuantityString()` is called, even with `0` or `1`.",
            Category.MESSAGES,
            3,
            Severity.WARNING,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/guide/topics/resources/string-resource.html#Plurals");

    /** This plural does not use the quantity value */
    public static final Issue IMPLIED_QUANTITY = Issue.create(
            "ImpliedQuantity", //$NON-NLS-1$
            "Implied Quantities",

            "Plural strings should generally include a `%s` or `%d` formatting argument. " +
            "In locales like English, the `one` quantity only applies to a single value, " +
            "1, but that's not true everywhere. For example, in Slovene, the `one` quantity " +
            "will apply to 1, 101, 201, 301, and so on. Similarly, there are locales where " +
            "multiple values match the `zero` and `two` quantities.\n" +
            "\n" +
            "In these locales, it is usually an error to have a message which does not " +
            "include a formatting argument (such as '%d'), since it will not be clear from " +
            "the grammar what quantity the quantity string is describing.",
            Category.MESSAGES,
            5,
            Severity.ERROR,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/guide/topics/resources/string-resource.html#Plurals");

    /** Constructs a new {@link PluralsDetector} */
    public PluralsDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_PLURALS);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        int count = LintUtils.getChildCount(element);
        if (count == 0) {
            context.report(MISSING, element, context.getLocation(element),
                    "There should be at least one quantity string in this `<plural>` definition");
            return;
        }

        LocaleQualifier locale = LintUtils.getLocale(context);
        if (locale == null || !locale.hasLanguage()) {
            return;
        }
        String language = locale.getLanguage();

        PluralsDatabase plurals = PluralsDatabase.get();

        EnumSet<Quantity> relevant = plurals.getRelevant(language);
        if (relevant == null) {
            return;
        }

        EnumSet<Quantity> defined = EnumSet.noneOf(Quantity.class);
        NodeList children = element.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node noe = children.item(i);
            if (noe.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) noe;
            if (!TAG_ITEM.equals(child.getTagName())) {
                continue;
            }

            String quantityString = child.getAttribute(ATTR_QUANTITY);
            if (quantityString == null || quantityString.isEmpty()) {
                continue;
            }
            Quantity quantity = Quantity.get(quantityString);
            if (quantity == null || quantity == Quantity.other) { // Not stored in the database
                continue;
            }
            defined.add(quantity);

            if (plurals.hasMultipleValuesForQuantity(language, quantity)
                    && !haveFormattingParameter(child) && context.isEnabled(IMPLIED_QUANTITY)) {
                String example = plurals.findIntegerExamples(language, quantity);
                String append;
                if (example == null) {
                    append = "";
                } else {
                    append = " (" + example + ")";
                }
                String message = String.format("The quantity `'%1$s'` matches more than one "
                                + "specific number in this locale%2$s, but the message did "
                                + "not include a formatting argument (such as `%%d`). "
                                + "This is usually an internationalization error. See full issue "
                                + "explanation for more.",
                        quantity, append);
                context.report(IMPLIED_QUANTITY, child, context.getLocation(child), message);
            }
        }

        if (relevant.equals(defined)) {
            return;
        }

        // Look for missing
        EnumSet<Quantity> missing = relevant.clone();
        missing.removeAll(defined);
        if (!missing.isEmpty()) {
            String message = String.format(
                    "For locale %1$s the following quantities should also be defined: %2$s",
                    TranslationDetector.getLanguageDescription(language),
                    Quantity.formatSet(missing));
            context.report(MISSING, element, context.getLocation(element), message);
        }

        // Look for irrelevant
        EnumSet<Quantity> extra = defined.clone();
        extra.removeAll(relevant);
        if (!extra.isEmpty()) {
            String message = String.format(
                    "For language %1$s the following quantities are not relevant: %2$s",
                    TranslationDetector.getLanguageDescription(language),
                    Quantity.formatSet(extra));
            context.report(EXTRA, element, context.getLocation(element), message);
        }
    }

    /**
     * Returns true if the given string/plurals item element contains a formatting parameter,
     * possibly within HTML markup or xliff metadata tags
     */
    private static boolean haveFormattingParameter(@NonNull Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            short nodeType = child.getNodeType();
            if (nodeType == Node.ELEMENT_NODE) {
                if (haveFormattingParameter((Element)child)) {
                    return true;
                }
            } else if (nodeType == Node.TEXT_NODE) {
                String text = child.getNodeValue();
                if (text.indexOf('%') == -1) {
                    continue;
                }
                if (StringFormatDetector.getFormatArgumentCount(text, null) >= 1) {
                    return true;
                }

            }
        }
        return false;
    }
}
