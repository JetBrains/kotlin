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

package com.android.tools.klint.checks;

import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.SdkConstants.CLASS_FRAGMENT;
import static com.android.SdkConstants.CLASS_RESOURCES;
import static com.android.SdkConstants.CLASS_V4_FRAGMENT;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.FORMAT_METHOD;
import static com.android.SdkConstants.GET_STRING_METHOD;
import static com.android.SdkConstants.TAG_STRING;
import static com.android.tools.klint.client.api.JavaParser.TYPE_BOOLEAN_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_BYTE_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_CHARACTER_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_DOUBLE_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_FLOAT_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_INTEGER_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_LONG_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_OBJECT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_SHORT_WRAPPER;
import static com.android.tools.klint.client.api.JavaParser.TYPE_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceUrl;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.client.api.LintClient;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Location.Handle;
import com.android.tools.klint.detector.api.Position;
import com.android.tools.klint.detector.api.ResourceEvaluator;
import com.android.tools.klint.detector.api.ResourceXmlDetector;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.UastVisitor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check which looks for problems with formatting strings such as inconsistencies between
 * translations or between string declaration and string usage in Java.
 * <p>
 * TODO: Handle Resources.getQuantityString as well
 */
public class StringFormatDetector extends ResourceXmlDetector implements Detector.UastScanner {
    private static final Implementation IMPLEMENTATION_XML = new Implementation(
            StringFormatDetector.class,
            Scope.ALL_RESOURCES_SCOPE);

    @SuppressWarnings("unchecked")
    private static final Implementation IMPLEMENTATION_XML_AND_JAVA = new Implementation(
            StringFormatDetector.class,
            EnumSet.of(Scope.ALL_RESOURCE_FILES, Scope.JAVA_FILE),
            Scope.JAVA_FILE_SCOPE);


    /** Whether formatting strings are invalid */
    public static final Issue INVALID = Issue.create(
            "StringFormatInvalid", //$NON-NLS-1$
            "Invalid format string",

            "If a string contains a '%' character, then the string may be a formatting string " +
            "which will be passed to `String.format` from Java code to replace each '%' " +
            "occurrence with specific values.\n" +
            "\n" +
            "This lint warning checks for two related problems:\n" +
            "(1) Formatting strings that are invalid, meaning that `String.format` will throw " +
            "exceptions at runtime when attempting to use the format string.\n" +
            "(2) Strings containing '%' that are not formatting strings getting passed to " +
            "a `String.format` call. In this case the '%' will need to be escaped as '%%'.\n" +
            "\n" +
            "NOTE: Not all Strings which look like formatting strings are intended for " +
            "use by `String.format`; for example, they may contain date formats intended " +
            "for `android.text.format.Time#format()`. Lint cannot always figure out that " +
            "a String is a date format, so you may get false warnings in those scenarios. " +
            "See the suppress help topic for information on how to suppress errors in " +
            "that case.",

            Category.MESSAGES,
            9,
            Severity.ERROR,
            IMPLEMENTATION_XML);

    /** Whether formatting argument types are consistent across translations */
    public static final Issue ARG_COUNT = Issue.create(
            "StringFormatCount", //$NON-NLS-1$
            "Formatting argument types incomplete or inconsistent",

            "When a formatted string takes arguments, it usually needs to reference the " +
            "same arguments in all translations (or all arguments if there are no " +
            "translations.\n" +
            "\n" +
            "There are cases where this is not the case, so this issue is a warning rather " +
            "than an error by default. However, this usually happens when a language is not " +
            "translated or updated correctly.",
            Category.MESSAGES,
            5,
            Severity.WARNING,
            IMPLEMENTATION_XML);

    /** Whether the string format supplied in a call to String.format matches the format string */
    public static final Issue ARG_TYPES = Issue.create(
            "StringFormatMatches", //$NON-NLS-1$
            "`String.format` string doesn't match the XML format string",

            "This lint check ensures the following:\n" +
            "(1) If there are multiple translations of the format string, then all translations " +
            "use the same type for the same numbered arguments\n" +
            "(2) The usage of the format string in Java is consistent with the format string, " +
            "meaning that the parameter types passed to String.format matches those in the " +
            "format string.",
            Category.MESSAGES,
            9,
            Severity.ERROR,
            IMPLEMENTATION_XML_AND_JAVA);

    /** This plural does not use the quantity value */
    public static final Issue POTENTIAL_PLURAL = Issue.create(
            "PluralsCandidate", //$NON-NLS-1$
            "Potential Plurals",

            "This lint check looks for potential errors in internationalization where you have " +
            "translated a message which involves a quantity and it looks like other parts of " +
            "the string may need grammatical changes.\n" +
            "\n" +
            "For example, rather than something like this:\n" +
            "  <string name=\"try_again\">Try again in %d seconds.</string>\n" +
            "you should be using a plural:\n" +
            "   <plurals name=\"try_again\">\n" +
            "        <item quantity=\"one\">Try again in %d second</item>\n" +
            "        <item quantity=\"other\">Try again in %d seconds</item>\n" +
            "    </plurals>\n" +
            "This will ensure that in other languages the right set of translations are " +
            "provided for the different quantity classes.\n" +
            "\n" +
            "(This check depends on some heuristics, so it may not accurately determine whether " +
            "a string really should be a quantity. You can use tools:ignore to filter out false " +
            "positives.",

            Category.MESSAGES,
            5,
            Severity.WARNING,
            IMPLEMENTATION_XML).addMoreInfo(
            "http://developer.android.com/guide/topics/resources/string-resource.html#Plurals");

    /**
     * Map from a format string name to a list of declaration file and actual
     * formatting string content. We're using a list since a format string can be
     * defined multiple times, usually for different translations.
     */
    private Map<String, List<Pair<Handle, String>>> mFormatStrings;

    /**
     * Map of strings that do not contain any formatting.
     */
    private final Map<String, Handle> mNotFormatStrings = new HashMap<String, Handle>();

    /**
     * Set of strings that have an unknown format such as date formatting; we should not
     * flag these as invalid when used from a String#format call
     */
    private Set<String> mIgnoreStrings;

    /** Constructs a new {@link StringFormatDetector} check */
    public StringFormatDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        if (LintUtils.endsWith(file.getName(), DOT_JAVA)) {
            return mFormatStrings != null;
        }

        return super.appliesTo(context, file);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_STRING);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        NodeList childNodes = element.getChildNodes();
        if (childNodes.getLength() > 0) {
            if (childNodes.getLength() == 1) {
                Node child = childNodes.item(0);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    checkTextNode(context, element, stripQuotes(child.getNodeValue()));
                }
            } else {
                // Concatenate children and build up a plain string.
                // This is needed to handle xliff localization documents,
                // but this needs more work so ignore compound XML documents as
                // string values for now:
                StringBuilder sb = new StringBuilder();
                addText(sb, element);
                if (sb.length() > 0) {
                    checkTextNode(context, element, sb.toString());
                }
            }
        }
    }

    private static void addText(StringBuilder sb, Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append(stripQuotes(node.getNodeValue().trim()));
        } else {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                addText(sb, childNodes.item(i));
            }
        }
    }

    /**
     * Removes all the unescaped quotes. See
     * <a href="http://developer.android.com/guide/topics/resources/string-resource.html#FormattingAndStyling">Escaping apostrophes and quotes</a>
     */
    @VisibleForTesting
    static String stripQuotes(String s) {
        StringBuilder sb = new StringBuilder();
        boolean isEscaped = false;
        boolean isQuotedBlock = false;
        for (int i = 0, len = s.length(); i < len; i++) {
            char current = s.charAt(i);
            if (isEscaped) {
                sb.append(current);
                isEscaped = false;
            } else {
                isEscaped = current == '\\'; // Next char will be escaped so we will just copy it
                if (current == '"') {
                    isQuotedBlock = !isQuotedBlock;
                } else if (current == '\'') {
                    if (isQuotedBlock) {
                        // We only add single quotes when they are within a quoted block
                        sb.append(current);
                    }
                } else {
                    sb.append(current);
                }
            }
        }

        return sb.toString();
    }

    private void checkTextNode(XmlContext context, Element element, String text) {
        String name = element.getAttribute(ATTR_NAME);
        boolean found = false;
        boolean foundPlural = false;

        // Look at the String and see if it's a format string (contains
        // positional %'s)
        for (int j = 0, m = text.length(); j < m; j++) {
            char c = text.charAt(j);
            if (c == '\\') {
                j++;
            }
            if (c == '%') {
                // Also make sure this String isn't an unformatted String
                String formatted = element.getAttribute("formatted"); //$NON-NLS-1$
                if (!formatted.isEmpty() && !Boolean.parseBoolean(formatted)) {
                    if (!mNotFormatStrings.containsKey(name)) {
                        Handle handle = context.createLocationHandle(element);
                        handle.setClientData(element);
                        mNotFormatStrings.put(name, handle);
                    }
                    return;
                }

                // See if it's not a format string, e.g. "Battery charge is 100%!".
                // If so we want to record this name in a special list such that we can
                // make sure you don't attempt to reference this string from a String.format
                // call.
                Matcher matcher = FORMAT.matcher(text);
                if (!matcher.find(j)) {
                    if (!mNotFormatStrings.containsKey(name)) {
                        Handle handle = context.createLocationHandle(element);
                        handle.setClientData(element);
                        mNotFormatStrings.put(name, handle);
                    }
                    return;
                }

                String conversion = matcher.group(6);
                int conversionClass = getConversionClass(conversion.charAt(0));
                if (conversionClass == CONVERSION_CLASS_UNKNOWN || matcher.group(5) != null) {
                    if (mIgnoreStrings == null) {
                        mIgnoreStrings = new HashSet<String>();
                    }
                    mIgnoreStrings.add(name);

                    // Don't process any other strings here; some of them could
                    // accidentally look like a string, e.g. "%H" is a hash code conversion
                    // in String.format (and hour in Time formatting).
                    return;
                }

                if (conversionClass == CONVERSION_CLASS_INTEGER && !foundPlural) {
                    // See if there appears to be further text content here.
                    // Look for whitespace followed by a letter, with no punctuation in between
                    for (int k = matcher.end(); k < m; k++) {
                        char nc = text.charAt(k);
                        if (!Character.isWhitespace(nc)) {
                            if (Character.isLetter(nc)) {
                                foundPlural = checkPotentialPlural(context, element, text, k);
                            }
                            break;
                        }
                    }
                }

                found = true;
                j++; // Ensure that when we process a "%%" we don't separately check the second %
            }
        }

        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        if (name != null) {
            Handle handle = context.createLocationHandle(element);
            handle.setClientData(element);
            if (found) {
                // Record it for analysis when seen in Java code
                if (mFormatStrings == null) {
                    mFormatStrings = new HashMap<String, List<Pair<Handle, String>>>();
                }

                List<Pair<Handle, String>> list = mFormatStrings.get(name);
                if (list == null) {
                    list = new ArrayList<Pair<Handle, String>>();
                    mFormatStrings.put(name, list);
                }
                list.add(Pair.of(handle, text));
            } else {
                if (!isReference(text)) {
                    mNotFormatStrings.put(name, handle);
                }
            }
        }
    }

    private static boolean isReference(@NonNull String text) {
        for (int i = 0, n = text.length(); i < n; i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c == '@' || c == '?';
            }
        }
        return false;
    }

    /**
     * Checks whether the text begins with a non-unit word, pointing to a string
     * that should probably be a plural instead. This
     */
    private static boolean checkPotentialPlural(XmlContext context, Element element, String text,
            int wordBegin) {
        // This method should only be called if the text is known to start with a word
        assert Character.isLetter(text.charAt(wordBegin));

        int wordEnd = wordBegin;
        while (wordEnd < text.length()) {
            if (!Character.isLetter(text.charAt(wordEnd))) {
                break;
            }
            wordEnd++;
        }

        // Eliminate units, since those are not sentences you need to use plurals for, e.g.
        //   "Elevation gain: %1$d m (%2$d ft)"
        // We'll determine whether something is a unit by looking for
        // (1) Multiple uppercase characters (e.g. KB, or MiB), or better yet, uppercase characters
        //     anywhere but as the first letter
        // (2) No vowels (e.g. ft)
        // (3) Adjacent consonants (e.g. ft); this one can eliminate some legitimate
        //     English words as well (e.g. "the") so we should really limit this to
        //     letter pairs that are not common in English. This is probably overkill
        //     so not handled yet. Instead we use a simpler heuristic:
        // (4) Very short "words" (1-2 letters)
        if (wordEnd - wordBegin <= 2) {
            // Very short word (1-2 chars): possible unit, e.g. "m", "ft", "kb", etc
            return false;
        }
        boolean hasVowel = false;
        for (int i = wordBegin; i < wordEnd; i++) {
            // Uppercase character anywhere but first character: probably a unit (e.g. KB)
            char c = text.charAt(i);
            if (i > wordBegin && Character.isUpperCase(c)) {
                return false;
            }
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y') {
                hasVowel = true;
            }
        }
        if (!hasVowel) {
            // No vowels: likely unit
            return false;
        }

        String word = text.substring(wordBegin, wordEnd);

        // Some other known abbreviations that we don't want to count:
        if (word.equals("min")) {
            return false;
        }

        // This heuristic only works in English!
        if (LintUtils.isEnglishResource(context, true)) {
            String message = String.format("Formatting %%d followed by words (\"%1$s\"): "
                            + "This should probably be a plural rather than a string", word);
            context.report(POTENTIAL_PLURAL, element,
                    context.getLocation(element),
                    message);
            // Avoid reporting multiple errors on the same string
            // (if it contains more than one %d)
            return true;
        }

        return false;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mFormatStrings != null) {
            boolean checkCount = context.isEnabled(ARG_COUNT);
            boolean checkValid = context.isEnabled(INVALID);
            boolean checkTypes = context.isEnabled(ARG_TYPES);

            // Ensure that all the format strings are consistent with respect to each other;
            // e.g. they all have the same number of arguments, they all use all the
            // arguments, and they all use the same types for all the numbered arguments
            for (Map.Entry<String, List<Pair<Handle, String>>> entry : mFormatStrings.entrySet()) {
                String name = entry.getKey();
                List<Pair<Handle, String>> list = entry.getValue();

                // Check argument counts
                if (checkCount) {
                    Handle notFormatted = mNotFormatStrings.get(name);
                    if (notFormatted != null) {
                        list = ImmutableList.<Pair<Handle, String>>builder()
                                .add(Pair.of(notFormatted, name)).addAll(list).build();
                    }
                    checkArity(context, name, list);
                }

                // Check argument types (and also make sure that the formatting strings are valid)
                if (checkValid || checkTypes) {
                    checkTypes(context, checkValid, checkTypes, name, list);
                }
            }
        }
    }

    private static void checkTypes(Context context, boolean checkValid,
            boolean checkTypes, String name, List<Pair<Handle, String>> list) {
        Map<Integer, String> types = new HashMap<Integer, String>();
        Map<Integer, Handle> typeDefinition = new HashMap<Integer, Handle>();
        for (Pair<Handle, String> pair : list) {
            Handle handle = pair.getFirst();
            String formatString = pair.getSecond();

            //boolean warned = false;
            Matcher matcher = FORMAT.matcher(formatString);
            int index = 0;
            int prevIndex = 0;
            int nextNumber = 1;
            while (true) {
                if (matcher.find(index)) {
                    int matchStart = matcher.start();
                    // Make sure this is not an escaped '%'
                    for (; prevIndex < matchStart; prevIndex++) {
                        char c = formatString.charAt(prevIndex);
                        if (c == '\\') {
                            prevIndex++;
                        }
                    }
                    if (prevIndex > matchStart) {
                        // We're in an escape, ignore this result
                        index = prevIndex;
                        continue;
                    }

                    index = matcher.end(); // Ensure loop proceeds
                    String str = formatString.substring(matchStart, matcher.end());
                    if (str.equals("%%") || str.equals("%n")) { //$NON-NLS-1$ //$NON-NLS-2$
                        // Just an escaped %
                        continue;
                    }

                    if (checkValid) {
                        // Make sure it's a valid format string
                        if (str.length() > 2 && str.charAt(str.length() - 2) == ' ') {
                            char last = str.charAt(str.length() - 1);
                            // If you forget to include the conversion character, e.g.
                            //   "Weight=%1$ g" instead of "Weight=%1$d g", then
                            // you're going to end up with a format string interpreted as
                            // "%1$ g". This means that the space character is interpreted
                            // as a flag character, but it can only be a flag character
                            // when used in conjunction with the numeric conversion
                            // formats (d, o, x, X). If that's not the case, make a
                            // dedicated error message
                            if (last != 'd' && last != 'o' && last != 'x' && last != 'X') {
                                Object clientData = handle.getClientData();
                                if (clientData instanceof Node) {
                                    if (context.getDriver().isSuppressed(null, INVALID,
                                            (Node) clientData)) {
                                        return;
                                    }
                                }

                                Location location = handle.resolve();
                                String message = String.format(
                                        "Incorrect formatting string `%1$s`; missing conversion " +
                                        "character in '`%2$s`' ?", name, str);
                                context.report(INVALID, location, message);
                                //warned = true;
                                continue;
                            }
                        }
                    }

                    if (!checkTypes) {
                        continue;
                    }

                    // Shouldn't throw a number format exception since we've already
                    // matched the pattern in the regexp
                    int number;
                    String numberString = matcher.group(1);
                    if (numberString != null) {
                        // Strip off trailing $
                        numberString = numberString.substring(0, numberString.length() - 1);
                        number = Integer.parseInt(numberString);
                        nextNumber = number + 1;
                    } else {
                        number = nextNumber++;
                    }
                    String format = matcher.group(6);
                    String currentFormat = types.get(number);
                    if (currentFormat == null) {
                        types.put(number, format);
                        typeDefinition.put(number, handle);
                    } else if (!currentFormat.equals(format)
                            && isIncompatible(currentFormat.charAt(0), format.charAt(0))) {

                        Object clientData = handle.getClientData();
                        if (clientData instanceof Node) {
                            if (context.getDriver().isSuppressed(null, ARG_TYPES,
                                    (Node) clientData)) {
                                return;
                            }
                        }

                        Location location = handle.resolve();
                        // Attempt to limit the location range to just the formatting
                        // string in question
                        location = refineLocation(context, location, formatString,
                                matcher.start(), matcher.end());
                        Location otherLocation = typeDefinition.get(number).resolve();
                        otherLocation.setMessage("Conflicting argument type here");
                        location.setSecondary(otherLocation);
                        File f = otherLocation.getFile();
                        String message = String.format(
                                "Inconsistent formatting types for argument #%1$d in " +
                                "format string `%2$s` ('%3$s'): Found both '`%4$s`' and '`%5$s`' " +
                                "(in %6$s)",
                                number, name,
                                str,
                                currentFormat, format,
                                f.getParentFile().getName() + File.separator + f.getName());
                        //warned = true;
                        context.report(ARG_TYPES, location, message);
                        break;
                    }
                } else {
                    break;
                }
            }

            // Check that the format string is valid by actually attempting to instantiate
            // it. We only do this if we haven't already complained about this string
            // for other reasons.
            /* Check disabled for now: it had many false reports due to conversion
             * errors (which is expected since we just pass in strings), but once those
             * are eliminated there aren't really any other valid error messages returned
             * (for example, calling the formatter with bogus formatting flags always just
             * returns a "conversion" error. It looks like we'd need to actually pass compatible
             * arguments to trigger other types of formatting errors such as precision errors.
            if (!warned && checkValid) {
                try {
                    formatter.format(formatString, "", "", "", "", "", "", "",
                            "", "", "", "", "", "", "");

                } catch (IllegalFormatException t) { // TODO: UnknownFormatConversionException
                    if (!t.getLocalizedMessage().contains(" != ")
                            && !t.getLocalizedMessage().contains("Conversion")) {
                        Location location = handle.resolve();
                        context.report(INVALID, location,
                                String.format("Wrong format for %1$s: %2$s",
                                        name, t.getLocalizedMessage()), null);
                    }
                }
            }
            */
        }
    }

    /**
     * Returns true if two String.format conversions are "incompatible" (meaning
     * that using these two for the same argument across different translations
     * is more likely an error than intentional. Some conversions are
     * incompatible, e.g. "d" and "s" where one is a number and string, whereas
     * others may work (e.g. float versus integer) but are probably not
     * intentional.
     */
    private static boolean isIncompatible(char conversion1, char conversion2) {
        int class1 = getConversionClass(conversion1);
        int class2 = getConversionClass(conversion2);
        return class1 != class2
                && class1 != CONVERSION_CLASS_UNKNOWN
                && class2 != CONVERSION_CLASS_UNKNOWN;
    }

    private static final int CONVERSION_CLASS_UNKNOWN = 0;
    private static final int CONVERSION_CLASS_STRING = 1;
    private static final int CONVERSION_CLASS_CHARACTER = 2;
    private static final int CONVERSION_CLASS_INTEGER = 3;
    private static final int CONVERSION_CLASS_FLOAT = 4;
    private static final int CONVERSION_CLASS_BOOLEAN = 5;
    private static final int CONVERSION_CLASS_HASHCODE = 6;
    private static final int CONVERSION_CLASS_PERCENT = 7;
    private static final int CONVERSION_CLASS_NEWLINE = 8;
    private static final int CONVERSION_CLASS_DATETIME = 9;

    private static int getConversionClass(char conversion) {
        // See http://developer.android.com/reference/java/util/Formatter.html
        switch (conversion) {
            case 't':   // Time/date conversion
            case 'T':
                return CONVERSION_CLASS_DATETIME;
            case 's':   // string
            case 'S':   // Uppercase string
                return CONVERSION_CLASS_STRING;
            case 'c':   // character
            case 'C':   // Uppercase character
                return CONVERSION_CLASS_CHARACTER;
            case 'd':   // decimal
            case 'o':   // octal
            case 'x':   // hex
            case 'X':
                return CONVERSION_CLASS_INTEGER;
            case 'f':   // decimal float
            case 'e':   // exponential float
            case 'E':
            case 'g':   // decimal or exponential depending on size
            case 'G':
            case 'a':   // hex float
            case 'A':
                return CONVERSION_CLASS_FLOAT;
            case 'b':   // boolean
            case 'B':
                return CONVERSION_CLASS_BOOLEAN;
            case 'h':   // boolean
            case 'H':
                return CONVERSION_CLASS_HASHCODE;
            case '%':   // literal
                return CONVERSION_CLASS_PERCENT;
            case 'n':   // literal
                return CONVERSION_CLASS_NEWLINE;
        }

        return CONVERSION_CLASS_UNKNOWN;
    }

    private static Location refineLocation(Context context, Location location,
            String formatString, int substringStart, int substringEnd) {
        Position startLocation = location.getStart();
        Position endLocation = location.getEnd();
        if (startLocation != null && endLocation != null) {
            int startOffset = startLocation.getOffset();
            int endOffset = endLocation.getOffset();
            if (startOffset >= 0) {
                String contents = context.getClient().readFile(location.getFile());
                if (endOffset <= contents.length() && startOffset < endOffset) {
                    int formatOffset = contents.indexOf(formatString, startOffset);
                    if (formatOffset != -1 && formatOffset <= endOffset) {
                        return Location.create(location.getFile(), contents,
                                formatOffset + substringStart, formatOffset + substringEnd);
                    }
                }
            }
        }

        return location;
    }

    /**
     * Check that the number of arguments in the format string is consistent
     * across translations, and that all arguments are used
     */
    private static void checkArity(Context context, String name, List<Pair<Handle, String>> list) {
        // Check to make sure that the argument counts and types are consistent
        int prevCount = -1;
        for (Pair<Handle, String> pair : list) {
            Set<Integer> indices = new HashSet<Integer>();
            int count = getFormatArgumentCount(pair.getSecond(), indices);
            Handle handle = pair.getFirst();
            if (prevCount != -1 && prevCount != count) {
                Object clientData = handle.getClientData();
                if (clientData instanceof Node) {
                    if (context.getDriver().isSuppressed(null, ARG_COUNT, (Node) clientData)) {
                        return;
                    }
                }
                Location location = handle.resolve();
                Location secondary = list.get(0).getFirst().resolve();
                secondary.setMessage("Conflicting number of arguments here");
                location.setSecondary(secondary);
                String message = String.format(
                        "Inconsistent number of arguments in formatting string `%1$s`; " +
                        "found both %2$d and %3$d", name, prevCount, count);
                context.report(ARG_COUNT, location, message);
                break;
            }

            for (int i = 1; i <= count; i++) {
                if (!indices.contains(i)) {
                    Object clientData = handle.getClientData();
                    if (clientData instanceof Node) {
                        if (context.getDriver().isSuppressed(null, ARG_COUNT,
                                (Node) clientData)) {
                            return;
                        }
                    }

                    Set<Integer> all = new HashSet<Integer>();
                    for (int j = 1; j < count; j++) {
                        all.add(j);
                    }
                    all.removeAll(indices);
                    List<Integer> sorted = new ArrayList<Integer>(all);
                    Collections.sort(sorted);
                    Location location = handle.resolve();
                    String message = String.format(
                            "Formatting string '`%1$s`' is not referencing numbered arguments %2$s",
                            name, sorted);
                    context.report(ARG_COUNT, location, message);
                    break;
                }
            }

            prevCount = count;
        }
    }

    // See java.util.Formatter docs
    public static final Pattern FORMAT = Pattern.compile(
            // Generic format:
            //   %[argument_index$][flags][width][.precision]conversion
            //
            "%" +                                                               //$NON-NLS-1$
            // Argument Index
            "(\\d+\\$)?" +                                                      //$NON-NLS-1$
            // Flags
            "([-+#, 0(<]*)?" +                                                  //$NON-NLS-1$
            // Width
            "(\\d+)?" +                                                         //$NON-NLS-1$
            // Precision
            "(\\.\\d+)?" +                                                      //$NON-NLS-1$
            // Conversion. These are all a single character, except date/time conversions
            // which take a prefix of t/T:
            "([tT])?" +                                                         //$NON-NLS-1$
            // The current set of conversion characters are
            // b,h,s,c,d,o,x,e,f,g,a,t (as well as all those as upper-case characters), plus
            // n for newlines and % as a literal %. And then there are all the time/date
            // characters: HIKLm etc. Just match on all characters here since there should
            // be at least one.
            "([a-zA-Z%])");                                                     //$NON-NLS-1$

    /** Given a format string returns the format type of the given argument */
    @VisibleForTesting
    @Nullable
    static String getFormatArgumentType(String s, int argument) {
        Matcher matcher = FORMAT.matcher(s);
        int index = 0;
        int prevIndex = 0;
        int nextNumber = 1;
        while (true) {
            if (matcher.find(index)) {
                String value = matcher.group(6);
                if ("%".equals(value) || "n".equals(value)) { //$NON-NLS-1$ //$NON-NLS-2$
                    index = matcher.end();
                    continue;
                }
                int matchStart = matcher.start();
                // Make sure this is not an escaped '%'
                for (; prevIndex < matchStart; prevIndex++) {
                    char c = s.charAt(prevIndex);
                    if (c == '\\') {
                        prevIndex++;
                    }
                }
                if (prevIndex > matchStart) {
                    // We're in an escape, ignore this result
                    index = prevIndex;
                    continue;
                }

                // Shouldn't throw a number format exception since we've already
                // matched the pattern in the regexp
                int number;
                String numberString = matcher.group(1);
                if (numberString != null) {
                    // Strip off trailing $
                    numberString = numberString.substring(0, numberString.length() - 1);
                    number = Integer.parseInt(numberString);
                    nextNumber = number + 1;
                } else {
                    number = nextNumber++;
                }

                if (number == argument) {
                    return matcher.group(6);
                }
                index = matcher.end();
            } else {
                break;
            }
        }

        return null;
    }

    /**
     * Given a format string returns the number of required arguments. If the
     * {@code seenArguments} parameter is not null, put the indices of any
     * observed arguments into it.
     */
    static int getFormatArgumentCount(@NonNull String s, @Nullable Set<Integer> seenArguments) {
        Matcher matcher = FORMAT.matcher(s);
        int index = 0;
        int prevIndex = 0;
        int nextNumber = 1;
        int max = 0;
        while (true) {
            if (matcher.find(index)) {
                String value = matcher.group(6);
                if ("%".equals(value) || "n".equals(value)) { //$NON-NLS-1$ //$NON-NLS-2$
                    index = matcher.end();
                    continue;
                }
                int matchStart = matcher.start();
                // Make sure this is not an escaped '%'
                for (; prevIndex < matchStart; prevIndex++) {
                    char c = s.charAt(prevIndex);
                    if (c == '\\') {
                        prevIndex++;
                    }
                }
                if (prevIndex > matchStart) {
                    // We're in an escape, ignore this result
                    index = prevIndex;
                    continue;
                }

                // Shouldn't throw a number format exception since we've already
                // matched the pattern in the regexp
                int number;
                String numberString = matcher.group(1);
                if (numberString != null) {
                    // Strip off trailing $
                    numberString = numberString.substring(0, numberString.length() - 1);
                    number = Integer.parseInt(numberString);
                    nextNumber = number + 1;
                } else {
                    number = nextNumber++;
                }

                if (number > max) {
                    max = number;
                }
                if (seenArguments != null) {
                    seenArguments.add(number);
                }

                index = matcher.end();
            } else {
                break;
            }
        }

        return max;
    }

    /**
     * Determines whether the given {@link String#format(String, Object...)}
     * formatting string is "locale dependent", meaning that its output depends
     * on the locale. This is the case if it for example references decimal
     * numbers of dates and times.
     *
     * @param format the format string
     * @return true if the format is locale sensitive, false otherwise
     */
    public static boolean isLocaleSpecific(@NonNull String format) {
        if (format.indexOf('%') == -1) {
            return false;
        }

        Matcher matcher = FORMAT.matcher(format);
        int index = 0;
        int prevIndex = 0;
        while (true) {
            if (matcher.find(index)) {
                int matchStart = matcher.start();
                // Make sure this is not an escaped '%'
                for (; prevIndex < matchStart; prevIndex++) {
                    char c = format.charAt(prevIndex);
                    if (c == '\\') {
                        prevIndex++;
                    }
                }
                if (prevIndex > matchStart) {
                    // We're in an escape, ignore this result
                    index = prevIndex;
                    continue;
                }

                String type = matcher.group(6);
                if (!type.isEmpty()) {
                    char t = type.charAt(0);

                    // The following formatting characters are locale sensitive:
                    switch (t) {
                        case 'd': // decimal integer
                        case 'e': // scientific
                        case 'E':
                        case 'f': // decimal float
                        case 'g': // general
                        case 'G':
                        case 't': // date/time
                        case 'T':
                            return true;
                    }
                }
                index = matcher.end();
            } else {
                break;
            }
        }

        return false;
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(FORMAT_METHOD, GET_STRING_METHOD);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression node, @NonNull UMethod method) {
        if (mFormatStrings == null && !context.getClient().supportsProjectResources()) {
            return;
        }

        JavaEvaluator evaluator = context.getEvaluator();
        String methodName = method.getName();
        if (methodName.equals(FORMAT_METHOD)) {
            if (JavaEvaluator.isMemberInClass(method, TYPE_STRING)) {
                // Check formatting parameters for
                //   java.lang.String#format(String format, Object... formatArgs)
                //   java.lang.String#format(Locale locale, String format, Object... formatArgs)
                checkStringFormatCall(context, method, node,
                        method.getParameterList().getParametersCount() == 3);

                // TODO: Consider also enforcing
                // java.util.Formatter#format(String string, Object... formatArgs)
            }
        } else {
            // Look up any of these string formatting methods:
            // android.content.res.Resources#getString(@StringRes int resId, Object... formatArgs)
            // android.content.Context#getString(@StringRes int resId, Object... formatArgs)
            // android.app.Fragment#getString(@StringRes int resId, Object... formatArgs)
            // android.support.v4.app.Fragment#getString(@StringRes int resId, Object... formatArgs)

            // Many of these also define a plain getString method:
            // android.content.res.Resources#getString(@StringRes int resId)
            // However, while it's possible that these contain formatting strings) it's
            // also possible that they're looking up strings that are not intended to be used
            // for formatting so while we may want to warn about this it's not necessarily
            // an error.
            if (method.getParameterList().getParametersCount() < 2) {
                return;
            }

            if (evaluator.isMemberInSubClassOf(method, CLASS_RESOURCES, false) ||
                    evaluator.isMemberInSubClassOf(method, CLASS_CONTEXT, false) ||
                    evaluator.isMemberInSubClassOf(method, CLASS_FRAGMENT, false) ||
                    evaluator.isMemberInSubClassOf(method, CLASS_V4_FRAGMENT, false)) {
                checkStringFormatCall(context, method, node, false);
            }

            // TODO: Consider also looking up
            // android.content.res.Resources#getQuantityString(@PluralsRes int id, int quantity,
            //              Object... formatArgs)
            // though this will require being smarter about cross referencing formatting
            // strings since we'll need to go via the quantity string definitions
        }
    }

    /**
     * Checks a String.format call that is using a string that doesn't contain format placeholders.
     * @param context the context to report errors to
     * @param call the AST node for the {@link String#format}
     * @param name the string name
     * @param handle the string location
     */
    private static void checkNotFormattedHandle(
            JavaContext context,
            UCallExpression call,
            String name,
            Handle handle) {
        Object clientData = handle.getClientData();
        if (clientData instanceof Node) {
            if (context.getDriver().isSuppressed(null, INVALID, (Node) clientData)) {
                return;
            }
        }
        Location location = context.getUastLocation(call);
        Location secondary = handle.resolve();
        secondary.setMessage("This definition does not require arguments");
        location.setSecondary(secondary);
        String message = String.format(
                "Format string '`%1$s`' is not a valid format string so it should not be " +
                        "passed to `String.format`",
                name);
        context.report(INVALID, call, location, message);
    }

    /**
     * Check the given String.format call (with the given arguments) to see if the string format is
     * being used correctly
     *  @param context           the context to report errors to
     * @param calledMethod      the method being called
     * @param call              the AST node for the {@link String#format}
     * @param specifiesLocale   whether the first parameter is a locale string, shifting the
     */
    private void checkStringFormatCall(
            JavaContext context,
            PsiMethod calledMethod,
            UCallExpression call,
            boolean specifiesLocale) {

        int argIndex = specifiesLocale ? 1 : 0;
        List<UExpression> args = call.getValueArguments();

        if (args.size() <= argIndex) {
            return;
        }

        UExpression argument = args.get(argIndex);
        ResourceUrl resource = ResourceEvaluator.getResource(context, argument);
        if (resource == null || resource.framework || resource.type != ResourceType.STRING) {
            return;
        }

        String name = resource.name;
        if (mIgnoreStrings != null && mIgnoreStrings.contains(name)) {
            return;
        }

        boolean passingVarArgsArray = false;
        int callCount = args.size() - 1 - argIndex;

        if (callCount == 1) {
            // If instead of a varargs call like
            //    getString(R.string.foo, arg1, arg2, arg3)
            // the code is calling the varargs method with a packed Object array, as in
            //    getString(R.string.foo, new Object[] { arg1, arg2, arg3 })
            // we'll need to handle that such that we don't think this is a single
            // argument

            UExpression lastArg = args.get(args.size() - 1);
            PsiParameterList parameterList = calledMethod.getParameterList();
            int parameterCount = parameterList.getParametersCount();
            if (parameterCount > 0 && parameterList.getParameters()[parameterCount - 1].isVarArgs()) {
                boolean knownArity = false;

                boolean argWasReference = false;
                if (lastArg instanceof UReferenceExpression) {
                    PsiElement resolved = ((UReferenceExpression) lastArg).resolve();
                    if (resolved instanceof PsiVariable) {
                        UExpression initializer = context.getUastContext().getInitializerBody (
                                (PsiVariable) resolved);
                        if (initializer != null &&
                            (UastExpressionUtils.isNewArray(initializer) ||
                             UastExpressionUtils.isArrayInitializer(initializer))) {
                            argWasReference = true;
                            // Now handled by check below
                            lastArg = initializer;
                        }
                    }
                }

                if (UastExpressionUtils.isNewArray(lastArg) ||
                    UastExpressionUtils.isArrayInitializer(lastArg)) {
                    UCallExpression arrayInitializer = (UCallExpression) lastArg;

                    if (UastExpressionUtils.isNewArrayWithInitializer(lastArg) ||
                        UastExpressionUtils.isArrayInitializer(lastArg)) {
                        callCount = arrayInitializer.getValueArgumentCount();
                        knownArity = true;
                    } else if (UastExpressionUtils.isNewArrayWithDimensions(lastArg)) {
                        List<UExpression> arrayDimensions = arrayInitializer.getValueArguments();
                        if (arrayDimensions.size() == 1) {
                            UExpression first = arrayDimensions.get(0);
                            if (first instanceof ULiteralExpression) {
                                Object o = ((ULiteralExpression) first).getValue();
                                if (o instanceof Integer) {
                                    callCount = (Integer)o;
                                    knownArity = true;
                                }
                            }
                        }
                    }

                    if (!knownArity) {
                        if (!argWasReference) {
                            return;
                        }
                    } else {
                        passingVarArgsArray = true;
                    }
                }
            }
        }

        if (callCount > 0 && mNotFormatStrings.containsKey(name)) {
            checkNotFormattedHandle(context, call, name, mNotFormatStrings.get(name));
            return;
        }

        List<Pair<Handle, String>> list = mFormatStrings != null ? mFormatStrings.get(name) : null;
        if (list == null) {
            LintClient client = context.getClient();
            if (client.supportsProjectResources() &&
                    !context.getScope().contains(Scope.RESOURCE_FILE)) {
                AbstractResourceRepository resources = client
                        .getProjectResources(context.getMainProject(), true);
                List<ResourceItem> items;
                if (resources != null) {
                    items = resources.getResourceItem(ResourceType.STRING, name);
                } else {
                    // Must be a non-Android module
                    items = null;
                }
                if (items != null) {
                    for (final ResourceItem item : items) {
                        ResourceValue v = item.getResourceValue(false);
                        if (v != null) {
                            String value = v.getRawXmlValue();
                            if (value != null) {
                                // Make sure it's really a formatting string,
                                // not for example "Battery remaining: 90%"
                                boolean isFormattingString = value.indexOf('%') != -1;
                                for (int j = 0, m = value.length();
                                        j < m && isFormattingString;
                                        j++) {
                                    char c = value.charAt(j);
                                    if (c == '\\') {
                                        j++;
                                    } else if (c == '%') {
                                        Matcher matcher = FORMAT.matcher(value);
                                        if (!matcher.find(j)) {
                                            isFormattingString = false;
                                        } else {
                                            String conversion = matcher.group(6);
                                            int conversionClass = getConversionClass(
                                                    conversion.charAt(0));
                                            if (conversionClass == CONVERSION_CLASS_UNKNOWN
                                                    || matcher.group(5) != null) {
                                                // Some date format etc - don't process
                                                return;
                                            }
                                        }
                                        j++; // Don't process second % in a %%
                                    }
                                    // If the user marked the string with
                                }

                                Handle handle = client.createResourceItemHandle(item);
                                if (isFormattingString) {
                                    if (list == null) {
                                        list = Lists.newArrayList();
                                        if (mFormatStrings == null) {
                                            mFormatStrings = Maps.newHashMap();
                                        }
                                        mFormatStrings.put(name, list);
                                    }
                                    list.add(Pair.of(handle, value));
                                } else if (callCount > 0) {
                                    checkNotFormattedHandle(context, call, name, handle);
                                }
                            }
                        }
                    }
                }
            } else {
                return;
            }
        }

        if (list != null) {
            Set<String> reported = null;
            for (Pair<Handle, String> pair : list) {
                String s = pair.getSecond();
                if (reported != null && reported.contains(s)) {
                    continue;
                }
                int count = getFormatArgumentCount(s, null);
                Handle handle = pair.getFirst();
                if (count != callCount) {
                    Location location = context.getUastLocation(call);
                    Location secondary = handle.resolve();
                    secondary.setMessage(String.format("This definition requires %1$d arguments",
                            count));
                    location.setSecondary(secondary);
                    String message = String.format(
                            "Wrong argument count, format string `%1$s` requires `%2$d` but format " +
                            "call supplies `%3$d`",
                            name, count, callCount);
                    context.report(ARG_TYPES, call, location, message);
                    if (reported == null) {
                        reported = Sets.newHashSet();
                    }
                    reported.add(s);
                } else {
                    if (passingVarArgsArray) {
                        // Can't currently check these: make sure we don't incorrectly
                        // flag parameters on the Object[] instead of the wrapped parameters
                        return;
                    }
                    for (int i = 1; i <= count; i++) {
                        int argumentIndex = i + argIndex;
                        PsiType type = args.get(argumentIndex).getExpressionType();
                        if (type != null) {
                            boolean valid = true;
                            String formatType = getFormatArgumentType(s, i);
                            if (formatType == null) {
                                continue;
                            }
                            char last = formatType.charAt(formatType.length() - 1);
                            if (formatType.length() >= 2 &&
                                    Character.toLowerCase(
                                            formatType.charAt(formatType.length() - 2)) == 't') {
                                // Date time conversion.
                                // TODO
                                continue;
                            }
                            switch (last) {
                                // Booleans. It's okay to pass objects to these;
                                // it will print "true" if non-null, but it's
                                // unusual and probably not intended.
                                case 'b':
                                case 'B':
                                    valid = isBooleanType(type);
                                    break;

                                // Numeric: integer and floats in various formats
                                case 'x':
                                case 'X':
                                case 'd':
                                case 'o':
                                case 'e':
                                case 'E':
                                case 'f':
                                case 'g':
                                case 'G':
                                case 'a':
                                case 'A':
                                    valid = isNumericType(type, true);
                                    break;
                                case 'c':
                                case 'C':
                                    // Unicode character
                                    valid = isCharacterType(type);
                                    break;
                                case 'h':
                                case 'H': // Hex print of hash code of objects
                                case 's':
                                case 'S':
                                    // String. Can pass anything, but warn about
                                    // numbers since you may have meant more
                                    // specific formatting. Use special issue
                                    // explanation for this?
                                    valid = !isBooleanType(type) &&
                                            !isNumericType(type, false);
                                    break;
                            }

                            if (!valid) {
                                Location location = context.getUastLocation(args.get(argumentIndex));
                                Location secondary = handle.resolve();
                                secondary.setMessage("Conflicting argument declaration here");
                                location.setSecondary(secondary);
                                String suggestion = null;
                                if (isBooleanType(type)) {
                                    suggestion = "`b`";
                                } else if (isCharacterType(type)) {
                                    suggestion = "'c'";
                                } else if (PsiType.INT.equals(type)
                                            || PsiType.LONG.equals(type)
                                            || PsiType.BYTE.equals(type)
                                            || PsiType.SHORT.equals(type)) {
                                    suggestion = "`d`, 'o' or `x`";
                                } else if (PsiType.FLOAT.equals(type)
                                        || PsiType.DOUBLE.equals(type)) {
                                    suggestion = "`e`, 'f', 'g' or `a`";
                                } else if (type instanceof PsiClassType) {
                                    String fqn = type.getCanonicalText();
                                    if (TYPE_INTEGER_WRAPPER.equals(fqn)
                                            || TYPE_LONG_WRAPPER.equals(fqn)
                                            || TYPE_BYTE_WRAPPER.equals(fqn)
                                            || TYPE_SHORT_WRAPPER.equals(fqn)) {
                                        suggestion = "`d`, 'o' or `x`";
                                    } else if (TYPE_FLOAT_WRAPPER.equals(fqn)
                                            || TYPE_DOUBLE_WRAPPER.equals(fqn)) {
                                        suggestion = "`d`, 'o' or `x`";
                                    } else if (TYPE_OBJECT.equals(fqn)) {
                                        suggestion = "'s' or 'h'";
                                    }
                                }

                                if (suggestion != null) {
                                    suggestion = " (Did you mean formatting character "
                                            + suggestion + "?)";
                                } else {
                                    suggestion = "";
                                }

                                String canonicalText = type.getCanonicalText();
                                canonicalText = canonicalText.substring(
                                        canonicalText.lastIndexOf('.') + 1);

                                String message = String.format(
                                        "Wrong argument type for formatting argument '#%1$d' " +
                                        "in `%2$s`: conversion is '`%3$s`', received `%4$s` " +
                                        "(argument #%5$d in method call)%6$s",
                                        i, name, formatType, canonicalText,
                                        argumentIndex + 1, suggestion);
                                context.report(ARG_TYPES, call, location, message);
                                if (reported == null) {
                                    reported = Sets.newHashSet();
                                }
                                reported.add(s);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isCharacterType(PsiType type) {
        //return PsiType.CHAR.isAssignableFrom(type);
        if (type == PsiType.CHAR) {
            return true;
        }
        if (type instanceof PsiClassType) {
            String fqn = type.getCanonicalText();
            return TYPE_CHARACTER_WRAPPER.equals(fqn);
        }

        return false;
    }

    private static boolean isBooleanType(PsiType type) {
        //return PsiType.BOOLEAN.isAssignableFrom(type);
        if (type == PsiType.BOOLEAN) {
            return true;
        }
        if (type instanceof PsiClassType) {
            String fqn = type.getCanonicalText();
            return TYPE_BOOLEAN_WRAPPER.equals(fqn);
        }

        return false;
    }

    //PsiType:java.lang.Boolean
    private static boolean isNumericType(@NonNull PsiType type, boolean allowBigNumbers) {
        if (PsiType.INT.equals(type)
                || PsiType.FLOAT.equals(type)
                || PsiType.DOUBLE.equals(type)
                || PsiType.LONG.equals(type)
                || PsiType.BYTE.equals(type)
                || PsiType.SHORT.equals(type)) {
            return true;
        }

        if (type instanceof PsiClassType) {
            String fqn = type.getCanonicalText();
            if (TYPE_INTEGER_WRAPPER.equals(fqn)
                    || TYPE_FLOAT_WRAPPER.equals(fqn)
                    || TYPE_DOUBLE_WRAPPER.equals(fqn)
                    || TYPE_LONG_WRAPPER.equals(fqn)
                    || TYPE_BYTE_WRAPPER.equals(fqn)
                    || TYPE_SHORT_WRAPPER.equals(fqn)) {
                return true;
            }
            if (allowBigNumbers) {
                if ("java.math.BigInteger".equals(fqn) ||
                        "java.math.BigDecimal".equals(fqn)) {
                    return true;
                }
            }
        }

        return false;
    }
}
