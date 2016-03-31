/*
 * Copyright (C) 2011 The Android Open Source Project
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

import static com.android.SdkConstants.ATTR_LOCALE;
import static com.android.SdkConstants.ATTR_TRANSLATABLE;
import static com.android.SdkConstants.FD_RES_VALUES;
import static com.android.SdkConstants.TAG_PLURALS;
import static com.android.SdkConstants.TAG_STRING;
import static com.android.SdkConstants.TAG_STRING_ARRAY;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.tools.lint.checks.TypoLookup.isLetter;
import static com.google.common.base.Objects.equal;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.configuration.LocaleQualifier;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.base.Charsets;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Check which looks for likely typos in Strings.
 * <p>
 * TODO:
 * <ul>
 * <li> Add check of Java String literals too!
 * <li> Add support for <b>additional</b> languages. The typo detector is now
 *      multilingual and looks for typos-*locale*.txt files to use. However,
 *      we need to seed it with additional typo databases. I did some searching
 *      and came up with some alternatives. Here's the strategy I used:
 *      Used Google Translate to translate "Wikipedia Common Misspellings", and
 *      then I went to google.no, google.fr etc searching with that translation, and
 *      came up with what looks like wikipedia language local lists of typos.
 *      This is how I found the Norwegian one for example:
 *      <br>
 *         http://no.wikipedia.org/wiki/Wikipedia:Liste_over_alminnelige_stavefeil/Maskinform
 *      <br>
 *     Here are some additional possibilities not yet processed:
 *      <ul>
 *        <li> French: http://fr.wikipedia.org/wiki/Wikip%C3%A9dia:Liste_de_fautes_d'orthographe_courantes
 *            (couldn't find a machine-readable version there?)
 *         <li> Swedish:
 *              http://sv.wikipedia.org/wiki/Wikipedia:Lista_%C3%B6ver_vanliga_spr%C3%A5kfel
 *              (couldn't find a machine-readable version there?)
 *        <li> German
 *              http://de.wikipedia.org/wiki/Wikipedia:Liste_von_Tippfehlern/F%C3%BCr_Maschinen
 *       </ul>
 * <li> Consider also digesting files like
 *       http://sv.wikipedia.org/wiki/Wikipedia:AutoWikiBrowser/Typos
 *       See http://en.wikipedia.org/wiki/Wikipedia:AutoWikiBrowser/User_manual.
 * </ul>
 */
public class TypoDetector extends ResourceXmlDetector {
    @Nullable private TypoLookup mLookup;
    @Nullable private String mLastLanguage;
    @Nullable private String mLastRegion;
    @Nullable private String mLanguage;
    @Nullable private String mRegion;

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "Typos", //$NON-NLS-1$
            "Spelling error",

            "This check looks through the string definitions, and if it finds any words " +
            "that look like likely misspellings, they are flagged.",
            Category.MESSAGES,
            7,
            Severity.WARNING,
            new Implementation(
                    TypoDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Constructs a new detector */
    public TypoDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    /** Look up the locale and region from the given parent folder name and store it
     * in {@link #mLanguage} and {@link #mRegion} */
    private void initLocale(@NonNull String parent) {
        mLanguage = null;
        mRegion = null;

        if (parent.equals(FD_RES_VALUES)) {
            return;
        }

        LocaleQualifier locale = LintUtils.getLocale(parent);
        if (locale != null) {
            mLanguage = locale.getLanguage();
            mRegion = locale.hasRegion() ? locale.getRegion() : null;
        }
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        initLocale(context.file.getParentFile().getName());
        if (mLanguage == null) {
            // Check to see if the user has specified the language for this folder
            // using a tools:locale attribute
            if (context instanceof XmlContext) {
                Element root = ((XmlContext) context).document.getDocumentElement();
                if (root != null) {
                    String locale = root.getAttributeNS(TOOLS_URI, ATTR_LOCALE);
                    if (locale != null && !locale.isEmpty()) {
                        initLocale(FD_RES_VALUES + '-' + locale);
                    }
                }
            }

            if (mLanguage == null) {
                mLanguage = "en"; //$NON-NLS-1$
            }
        }

        if (!equal(mLastLanguage, mLanguage) || !equal(mLastRegion, mRegion)) {
            mLookup = TypoLookup.get(context.getClient(), mLanguage, mRegion);
            mLastLanguage = mLanguage;
            mLastRegion = mRegion;
        }
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_STRING,
                TAG_STRING_ARRAY,
                TAG_PLURALS
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (mLookup == null) {
            return;
        }

        visit(context, element, element);
    }

    private void visit(XmlContext context, Element parent, Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            // TODO: Figure out how to deal with entities
            check(context, parent, node, node.getNodeValue());
        } else {
            NodeList children = node.getChildNodes();
            for (int i = 0, n = children.getLength(); i < n; i++) {
                visit(context, parent, children.item(i));
            }
        }
    }

    private void check(XmlContext context, Element element, Node node, String text) {
        int max = text.length();
        int index = 0;
        int lastWordBegin = -1;
        int lastWordEnd = -1;
        boolean checkedTypos = false;

        for (; index < max; index++) {
            char c = text.charAt(index);
            if (!Character.isWhitespace(c)) {
                if (c == '@' || (c == '?')) {
                    // Don't look for typos in resource references; they are not
                    // user visible anyway
                    return;
                }
                break;
            }
        }

        while (index < max) {
            for (; index < max; index++) {
                char c = text.charAt(index);
                if (c == '\\') {
                    index++;
                } else if (Character.isLetter(c)) {
                    break;
                }
            }
            if (index >= max) {
                return;
            }
            int begin = index;
            for (; index < max; index++) {
                char c = text.charAt(index);
                if (c == '\\') {
                    index++;
                    break;
                } else if (!Character.isLetter(c)) {
                    break;
                } else if (text.charAt(index) >= 0x80) {
                    // Switch to UTF-8 handling for this string
                    if (checkedTypos) {
                        // If we've already checked words we may have reported typos
                        // so create a substring from the current word and on.
                        byte[] utf8Text = text.substring(begin).getBytes(Charsets.UTF_8);
                        check(context, element, node, utf8Text, 0, utf8Text.length, text, begin);
                    } else {
                        // If all we've done so far is skip whitespace (common scenario)
                        // then no need to substring the text, just re-search with the
                        // UTF-8 routines
                        byte[] utf8Text = text.getBytes(Charsets.UTF_8);
                        check(context, element, node, utf8Text, 0, utf8Text.length, text, 0);
                    }
                    return;
                }
            }

            int end = index;
            checkedTypos = true;
            assert mLookup != null;
            List<String> replacements = mLookup.getTypos(text, begin, end);
            if (replacements != null && isTranslatable(element)) {
                reportTypo(context, node, text, begin, replacements);
            }

            checkRepeatedWords(context, element, node, text, lastWordBegin, lastWordEnd, begin,
                    end);

            lastWordBegin = begin;
            lastWordEnd = end;
            index = end + 1;
        }
    }

    private static void checkRepeatedWords(XmlContext context, Element element, Node node,
            String text, int lastWordBegin, int lastWordEnd, int begin, int end) {
        if (lastWordBegin != -1 && end - begin == lastWordEnd - lastWordBegin
                && end - begin > 1) {
            // See whether we have a repeated word
            boolean different = false;
            for (int i = lastWordBegin, j = begin; i < lastWordEnd; i++, j++) {
                if (text.charAt(i) != text.charAt(j)) {
                    different = true;
                    break;
                }
            }
            if (!different && onlySpace(text, lastWordEnd, begin) && isTranslatable(element)) {
                reportRepeatedWord(context, node, text, lastWordBegin, begin, end);
            }
        }
    }

    private static boolean onlySpace(String text, int fromInclusive, int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private void check(XmlContext context, Element element, Node node, byte[] utf8Text,
            int byteStart, int byteEnd, String text, int charStart) {
        int lastWordBegin = -1;
        int lastWordEnd = -1;
        int index = byteStart;
        while (index < byteEnd) {
            // Find beginning of word
            while (index < byteEnd) {
                byte b = utf8Text[index];
                if (b == '\\') {
                    index++;
                    charStart++;
                    if (index < byteEnd) {
                        b = utf8Text[index];
                    }
                } else if (isLetter(b)) {
                    break;
                }
                index++;
                if ((b & 0x80) == 0 || (b & 0xC0) == 0xC0) {
                    // First characters in UTF-8 are always ASCII (0 high bit) or 11XXXXXX
                    charStart++;
                }
            }

            if (index >= byteEnd) {
                return;
            }
            int charEnd = charStart;
            int begin = index;

            // Find end of word. Unicode has the nice property that even 2nd, 3rd and 4th
            // bytes won't match these ASCII characters (because the high bit must be set there)
            while (index < byteEnd) {
                byte b = utf8Text[index];
                if (b == '\\') {
                    index++;
                    charEnd++;
                    if (index < byteEnd) {
                        b = utf8Text[index++];
                        if ((b & 0x80) == 0 || (b & 0xC0) == 0xC0) {
                            charEnd++;
                        }
                    }
                    break;
                } else if (!isLetter(b)) {
                    break;
                }
                index++;
                if ((b & 0x80) == 0 || (b & 0xC0) == 0xC0) {
                    // First characters in UTF-8 are always ASCII (0 high bit) or 11XXXXXX
                    charEnd++;
                }
            }

            int end = index;
            List<String> replacements = mLookup.getTypos(utf8Text, begin, end);
            if (replacements != null && isTranslatable(element)) {
                reportTypo(context, node, text, charStart, replacements);
            }

            checkRepeatedWords(context, element, node, text, lastWordBegin, lastWordEnd, charStart,
                    charEnd);

            lastWordBegin = charStart;
            lastWordEnd = charEnd;
            charStart = charEnd;
        }
    }

    private static boolean isTranslatable(Element element) {
        Attr translatable = element.getAttributeNode(ATTR_TRANSLATABLE);
        return translatable == null || Boolean.valueOf(translatable.getValue());
    }

    /** Report the typo found at the given offset and suggest the given replacements */
    private static void reportTypo(XmlContext context, Node node, String text, int begin,
            List<String> replacements) {
        if (replacements.size() < 2) {
            return;
        }

        String typo = replacements.get(0);
        String word = text.substring(begin, begin + typo.length());

        String first = null;
        String message;

        boolean isCapitalized = Character.isUpperCase(word.charAt(0));
        StringBuilder sb = new StringBuilder(40);
        for (int i = 1, n = replacements.size(); i < n; i++) {
            String replacement = replacements.get(i);
            if (first == null) {
                first = replacement;
            }
            if (sb.length() > 0) {
                sb.append(" or ");
            }
            sb.append('"');
            if (isCapitalized) {
                sb.append(Character.toUpperCase(replacement.charAt(0)));
                sb.append(replacement.substring(1));
            } else {
                sb.append(replacement);
            }
            sb.append('"');
        }

        if (first != null && first.equalsIgnoreCase(word)) {
            if (first.equals(word)) {
                return;
            }
            message = String.format(
                    "\"%1$s\" is usually capitalized as \"%2$s\"",
                    word, first);
        } else {
            message = String.format(
                    "\"%1$s\" is a common misspelling; did you mean %2$s ?",
                    word, sb.toString());
        }

        int end = begin + word.length();
        context.report(ISSUE, node, context.getLocation(node, begin, end), message);
    }

    /** Reports a repeated word */
    private static void reportRepeatedWord(XmlContext context, Node node, String text,
            int lastWordBegin,
            int begin, int end) {
        String message = String.format(
                "Repeated word \"%1$s\" in message: possible typo",
                text.substring(begin, end));
        Location location = context.getLocation(node, lastWordBegin, end);
        context.report(ISSUE, node, location, message);
    }

    /** Returns the suggested replacements, if any, for the given typo. The error
     * message <b>must</b> be one supplied by lint.
     *
     * @param errorMessage the error message
     * @param format the format of the error message
     * @return a list of replacement words suggested by the error message
     */
    @Nullable
    public static List<String> getSuggestions(@NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);

        // The words are all in quotes; the first word is the misspelling,
        // the other words are the suggested replacements
        List<String> words = new ArrayList<String>();
        // Skip the typo
        int index = errorMessage.indexOf('"');
        index = errorMessage.indexOf('"', index + 1);
        index++;

        while (true) {
            index = errorMessage.indexOf('"', index);
            if (index == -1) {
                break;
            }
            index++;
            int start = index;
            index = errorMessage.indexOf('"', index);
            if (index == -1) {
                index = errorMessage.length();
            }
            words.add(errorMessage.substring(start, index));
            index++;
        }

        return words;
    }

    /**
     * Returns the typo word in the error message from this detector
     *
     * @param errorMessage the error message produced earlier by this detector
     * @param format the format of the error message
     * @return the typo
     */
    @Nullable
    public static String getTypo(@NonNull String errorMessage, @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);
        // The words are all in quotes
        int index = errorMessage.indexOf('"');
        int start = index + 1;
        index = errorMessage.indexOf('"', start);
        if (index != -1) {
            return errorMessage.substring(start, index);
        }

        return null;
    }
}
