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

import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.TAG_PLURALS;
import static com.android.SdkConstants.TAG_STRING;
import static com.android.SdkConstants.TAG_STRING_ARRAY;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for various typographical issues in string definitions.
 */
public class TypographyDetector extends ResourceXmlDetector {

    private static final Implementation IMPLEMENTATION = new Implementation(
            TypographyDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Replace hyphens with dashes? */
    public static final Issue DASHES = Issue.create(
            "TypographyDashes", //$NON-NLS-1$
            "Hyphen can be replaced with dash",
            "The \"n dash\" (\u2013, &#8211;) and the \"m dash\" (\u2014, &#8212;) " +
            "characters are used for ranges (n dash) and breaks (m dash). Using these " +
            "instead of plain hyphens can make text easier to read and your application " +
            "will look more polished.",
            Category.TYPOGRAPHY,
            5,
            Severity.WARNING,
            IMPLEMENTATION).
            addMoreInfo("http://en.wikipedia.org/wiki/Dash"); //$NON-NLS-1$

    /** Replace dumb quotes with smart quotes? */
    public static final Issue QUOTES = Issue.create(
            "TypographyQuotes", //$NON-NLS-1$
            "Straight quotes can be replaced with curvy quotes",
            "Straight single quotes and double quotes, when used as a pair, can be replaced " +
            "by \"curvy quotes\" (or directional quotes). This can make the text more " +
            "readable.\n" +
            "\n" +
            "Note that you should never use grave accents and apostrophes to quote, " +
            "`like this'.\n" +
            "\n" +
            "(Also note that you should not use curvy quotes for code fragments.)",
            Category.TYPOGRAPHY,
            5,
            Severity.WARNING,
            IMPLEMENTATION).
            addMoreInfo("http://en.wikipedia.org/wiki/Quotation_mark"). //$NON-NLS-1$
            // This feature is apparently controversial: recent apps have started using
            // straight quotes to avoid inconsistencies. Disabled by default for now.
            setEnabledByDefault(false);

    /** Replace fraction strings with fraction characters? */
    public static final Issue FRACTIONS = Issue.create(
            "TypographyFractions", //$NON-NLS-1$
            "Fraction string can be replaced with fraction character",
            "You can replace certain strings, such as 1/2, and 1/4, with dedicated " +
            "characters for these, such as \u00BD (&#189;) and \u00BC (&#188;). " +
            "This can help make the text more readable.",
            Category.TYPOGRAPHY,
            5,
            Severity.WARNING,
            IMPLEMENTATION).
            addMoreInfo("http://en.wikipedia.org/wiki/Number_Forms"); //$NON-NLS-1$

    /** Replace ... with the ellipsis character? */
    public static final Issue ELLIPSIS = Issue.create(
            "TypographyEllipsis", //$NON-NLS-1$
            "Ellipsis string can be replaced with ellipsis character",
            "You can replace the string \"...\" with a dedicated ellipsis character, " +
            "ellipsis character (\u2026, &#8230;). This can help make the text more readable.",
            Category.TYPOGRAPHY,
            5,
            Severity.WARNING,
            IMPLEMENTATION).
            addMoreInfo("http://en.wikipedia.org/wiki/Ellipsis"); //$NON-NLS-1$

    /** The main issue discovered by this detector */
    public static final Issue OTHER = Issue.create(
            "TypographyOther", //$NON-NLS-1$
            "Other typographical problems",
            "This check looks for miscellaneous typographical problems and offers replacement " +
            "sequences that will make the text easier to read and your application more " +
            "polished.",
            Category.TYPOGRAPHY,
            3,
            Severity.WARNING,
            IMPLEMENTATION);

    private static final String GRAVE_QUOTE_MESSAGE =
        "Avoid quoting with grave accents; use apostrophes or better yet directional quotes instead";
    private static final String ELLIPSIS_MESSAGE =
        "Replace \"...\" with ellipsis character (\u2026, &#8230;) ?";
    private static final String EN_DASH_MESSAGE =
        "Replace \"-\" with an \"en dash\" character (\u2013, &#8211;) ?";
    private static final String EM_DASH_MESSAGE =
        "Replace \"--\" with an \"em dash\" character (\u2014, &#8212;) ?";
    private static final String TYPOGRAPHIC_APOSTROPHE_MESSAGE =
        "Replace apostrophe (') with typographic apostrophe (\u2019, &#8217;) ?";
    private static final String SINGLE_QUOTE_MESSAGE =
        "Replace straight quotes ('') with directional quotes (\u2018\u2019, &#8216; and &#8217;) ?";
    private static final String DBL_QUOTES_MESSAGE =
        "Replace straight quotes (\") with directional quotes (\u201C\u201D, &#8220; and &#8221;) ?";
    private static final String COPYRIGHT_MESSAGE =
        "Replace (c) with copyright symbol \u00A9 (&#169;) ?";

    /**
     * Pattern used to detect scenarios which can be replaced with n dashes: a
     * numeric range with a hyphen in the middle (and possibly spaces)
     */
    @VisibleForTesting
    static final Pattern HYPHEN_RANGE_PATTERN =
            Pattern.compile(".*(\\d+\\s*)-(\\s*\\d+).*"); //$NON-NLS-1$

    /**
     * Pattern used to detect scenarios where a grave accent mark is used
     * to do ASCII quotations of the form `this'' or ``this'', which is frowned upon.
     * This pattern tries to avoid falsely complaining about strings like
     * "Type Option-` then 'Escape'."
     */
    @VisibleForTesting
    static final Pattern GRAVE_QUOTATION =
            Pattern.compile("(^[^`]*`[^'`]+'[^']*$)|(^[^`]*``[^'`]+''[^']*$)"); //$NON-NLS-1$

    /**
     * Pattern used to detect common fractions, e.g. 1/2, 1/3, 2/3, 1/4, 3/4 and
     * variations like 2 / 3, but not 11/22 and so on.
     */
    @VisibleForTesting
    static final Pattern FRACTION_PATTERN =
            Pattern.compile(".*\\b([13])\\s*/\\s*([234])\\b.*"); //$NON-NLS-1$

    /**
     * Pattern used to detect single quote strings, such as 'hello', but
     * not just quoted strings like 'Double quote: "', and not sentences
     * where there are multiple apostrophes but not in a quoting context such
     * as "Mind Your P's and Q's".
     */
    @VisibleForTesting
    static final Pattern SINGLE_QUOTE =
            Pattern.compile(".*\\W*'[^']+'(\\W.*)?"); //$NON-NLS-1$

    private static final String FRACTION_MESSAGE =
            "Use fraction character %1$c (%2$s) instead of %3$s ?";

    private static final String FRACTION_MESSAGE_PATTERN =
            "Use fraction character (.+) \\((.+)\\) instead of (.+) \\?";

    private boolean mCheckDashes;
    private boolean mCheckQuotes;
    private boolean mCheckFractions;
    private boolean mCheckEllipsis;
    private boolean mCheckMisc;

    /** Constructs a new {@link TypographyDetector} */
    public TypographyDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
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
    public void beforeCheckProject(@NonNull Context context) {
        mCheckDashes = context.isEnabled(DASHES);
        mCheckQuotes = context.isEnabled(QUOTES);
        mCheckFractions = context.isEnabled(FRACTIONS);
        mCheckEllipsis = context.isEnabled(ELLIPSIS);
        mCheckMisc = context.isEnabled(OTHER);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getNodeValue();
                checkText(context, element, child, text);
            } else if (child.getNodeType() == Node.ELEMENT_NODE &&
                    (child.getParentNode().getNodeName().equals(TAG_STRING_ARRAY) ||
                            child.getParentNode().getNodeName().equals(TAG_PLURALS))) {
                // String array or plural item children
                NodeList items = child.getChildNodes();
                for (int j = 0, m = items.getLength(); j < m; j++) {
                    Node item = items.item(j);
                    if (item.getNodeType() == Node.TEXT_NODE) {
                        String text = item.getNodeValue();
                        checkText(context, child, item, text);
                    }
                }
            }
        }
    }

    private void checkText(XmlContext context, Node element, Node textNode, String text) {
        if (mCheckEllipsis) {
            // Replace ... with ellipsis character?
            int ellipsis = text.indexOf("..."); //$NON-NLS-1$
            if (ellipsis != -1 && !text.startsWith(".", ellipsis + 3)) { //$NON-NLS-1$
                context.report(ELLIPSIS, element, context.getLocation(textNode),
                        ELLIPSIS_MESSAGE);
            }
        }

        // Dashes
        if (mCheckDashes) {
            int hyphen = text.indexOf('-');
            if (hyphen != -1) {
                // n dash
                Matcher matcher = HYPHEN_RANGE_PATTERN.matcher(text);
                if (matcher.matches()) {
                    // Make sure that if there is no space before digit there isn't
                    // one on the left either -- since we don't want to consider
                    // "1 2 -3" as a range from 2 to 3
                    boolean isNegativeNumber =
                        !Character.isWhitespace(matcher.group(2).charAt(0)) &&
                            Character.isWhitespace(matcher.group(1).charAt(
                                    matcher.group(1).length() - 1));
                    if (!isNegativeNumber && !isAnalyticsTrackingId((Element) element)) {
                        context.report(DASHES, element, context.getLocation(textNode),
                            EN_DASH_MESSAGE);
                    }
                }

                // m dash
                int emdash = text.indexOf("--"); //$NON-NLS-1$
                // Don't suggest replacing -- or "--" with an m dash since these are sometimes
                // used as digit marker strings
                if (emdash > 1 && !text.startsWith("-", emdash + 2)) {   //$NON-NLS-1$
                    context.report(DASHES, element, context.getLocation(textNode),
                            EM_DASH_MESSAGE);
                }
            }
        }

        if (mCheckQuotes) {
            // Check for single quotes that can be replaced with directional quotes
            int quoteStart = text.indexOf('\'');
            if (quoteStart != -1) {
                int quoteEnd = text.indexOf('\'', quoteStart + 1);
                if (quoteEnd != -1 && quoteEnd > quoteStart + 1
                        && (quoteEnd < text.length() -1 || quoteStart > 0)
                        && SINGLE_QUOTE.matcher(text).matches()) {
                    context.report(QUOTES, element, context.getLocation(textNode),
                        SINGLE_QUOTE_MESSAGE);
                    return;
                }

                // Check for apostrophes that can be replaced by typographic apostrophes
                if (quoteEnd == -1 && quoteStart > 0
                        && Character.isLetterOrDigit(text.charAt(quoteStart - 1))) {
                    context.report(QUOTES, element, context.getLocation(textNode),
                            TYPOGRAPHIC_APOSTROPHE_MESSAGE);
                    return;
                }
            }

            // Check for double quotes that can be replaced by directional double quotes
            quoteStart = text.indexOf('"');
            if (quoteStart != -1) {
                int quoteEnd = text.indexOf('"', quoteStart + 1);
                if (quoteEnd != -1 && quoteEnd > quoteStart + 1) {
                    if (quoteEnd < text.length() -1 || quoteStart > 0) {
                        context.report(QUOTES, element, context.getLocation(textNode),
                            DBL_QUOTES_MESSAGE);
                        return;
                    }
                }
            }

            // Check for grave accent quotations
            if (text.indexOf('`') != -1 && GRAVE_QUOTATION.matcher(text).matches()) {
                // Are we indenting ``like this'' or `this' ? If so, complain
                context.report(QUOTES, element, context.getLocation(textNode),
                        GRAVE_QUOTE_MESSAGE);
                return;
            }

            // Consider suggesting other types of directional quotes, such as guillemets, in
            // other languages?
            // There are a lot of exceptions and special cases to be considered so
            // this will need careful implementation and testing.
            // See http://en.wikipedia.org/wiki/Non-English_usage_of_quotation_marks
        }

        // Fraction symbols?
        if (mCheckFractions && text.indexOf('/') != -1) {
            Matcher matcher = FRACTION_PATTERN.matcher(text);
            if (matcher.matches()) {
                String top = matcher.group(1);    // Numerator
                String bottom = matcher.group(2); // Denominator
                if (top.equals("1") && bottom.equals("2")) { //$NON-NLS-1$ //$NON-NLS-2$
                    context.report(FRACTIONS, element, context.getLocation(textNode),
                            String.format(FRACTION_MESSAGE, '\u00BD', "&#189;", "1/2"));
                } else if (top.equals("1") && bottom.equals("4")) { //$NON-NLS-1$ //$NON-NLS-2$
                    context.report(FRACTIONS, element, context.getLocation(textNode),
                            String.format(FRACTION_MESSAGE, '\u00BC', "&#188;", "1/4"));
                } else if (top.equals("3") && bottom.equals("4")) { //$NON-NLS-1$ //$NON-NLS-2$
                    context.report(FRACTIONS, element, context.getLocation(textNode),
                            String.format(FRACTION_MESSAGE, '\u00BE', "&#190;", "3/4"));
                } else if (top.equals("1") && bottom.equals("3")) { //$NON-NLS-1$ //$NON-NLS-2$
                    context.report(FRACTIONS, element, context.getLocation(textNode),
                            String.format(FRACTION_MESSAGE, '\u2153', "&#8531;", "1/3"));
                } else if (top.equals("2") && bottom.equals("3")) { //$NON-NLS-1$ //$NON-NLS-2$
                    context.report(FRACTIONS, element, context.getLocation(textNode),
                            String.format(FRACTION_MESSAGE, '\u2154', "&#8532;", "2/3"));
                }
            }
        }

        if (mCheckMisc) {
            // Fix copyright symbol?
            if (text.indexOf('(') != -1
                    && (text.contains("(c)") || text.contains("(C)"))) { //$NON-NLS-1$ //$NON-NLS-2$
                // Suggest replacing with copyright symbol?
                context.report(OTHER, element, context.getLocation(textNode), COPYRIGHT_MESSAGE);
                // Replace (R) and TM as well? There are unicode characters for these but they
                // are probably not very common within Android app strings.
            }
        }
    }

    private static boolean isAnalyticsTrackingId(Element element) {
        String name = element.getAttribute(ATTR_NAME);
        return "ga_trackingId".equals(name); //$NON-NLS-1$
    }

    /**
     * An object describing a single edit to be made. The offset points to a
     * location to start editing; the length is the number of characters to
     * delete, and the replaceWith string points to a string to insert at the
     * offset. Note that this can model not just replacement edits but deletions
     * (empty replaceWith) and insertions (replace length = 0) too.
     */
    public static class ReplaceEdit {
        /** The offset of the edit */
        public final int offset;
        /** The number of characters to delete at the offset */
        public final int length;
        /** The characters to insert at the offset */
        public final String replaceWith;

        /**
         * Creates a new replace edit
         *
         * @param offset the offset of the edit
         * @param length the number of characters to delete at the offset
         * @param replaceWith the characters to insert at the offset
         */
        public ReplaceEdit(int offset, int length, String replaceWith) {
            super();
            this.offset = offset;
            this.length = length;
            this.replaceWith = replaceWith;
        }
    }

    /**
     * Returns a list of edits to be applied to fix the suggestion made by the
     * given warning. The specific issue id and message should be the message
     * provided by this detector in an earlier run.
     * <p>
     * This is intended to help tools implement automatic fixes of these
     * warnings. The reason only the message and issue id can be provided
     * instead of actual state passed in the data field to a reporter is that
     * fix operation can be run much later than the lint is processed (for
     * example, in a subsequent run of the IDE when only the warnings have been
     * persisted),
     *
     * @param issueId the issue id, which should be the id for one of the
     *            typography issues
     * @param message the actual error message, which should be a message
     *            provided by this detector
     * @param textNode a text node which corresponds to the text node the
     *            warning operated on
     * @return a list of edits, which is never null but could be empty. The
     *         offsets in the edit objects are relative to the text node.
     */
    public static List<ReplaceEdit> getEdits(String issueId, String message, Node textNode) {
      return getEdits(issueId, message, textNode.getNodeValue());
    }

  /**
   * Returns a list of edits to be applied to fix the suggestion made by the
   * given warning. The specific issue id and message should be the message
   * provided by this detector in an earlier run.
   * <p>
   * This is intended to help tools implement automatic fixes of these
   * warnings. The reason only the message and issue id can be provided
   * instead of actual state passed in the data field to a reporter is that
   * fix operation can be run much later than the lint is processed (for
   * example, in a subsequent run of the IDE when only the warnings have been
   * persisted),
   *
   * @param issueId the issue id, which should be the id for one of the
   *            typography issues
   * @param message the actual error message, which should be a message
   *            provided by this detector
   * @param text the text of the XML node where the warning appeared
   * @return a list of edits, which is never null but could be empty. The
   *         offsets in the edit objects are relative to the text node.
   */
    public static List<ReplaceEdit> getEdits(String issueId, String message, String text) {
        List<ReplaceEdit> edits = new ArrayList<ReplaceEdit>();
        if (message.equals(ELLIPSIS_MESSAGE)) {
            int offset = text.indexOf("...");                            //$NON-NLS-1$
            if (offset != -1) {
                edits.add(new ReplaceEdit(offset, 3, "\u2026"));         //$NON-NLS-1$
            }
        } else if (message.equals(EN_DASH_MESSAGE)) {
            int offset = text.indexOf('-');
            if (offset != -1) {
                edits.add(new ReplaceEdit(offset, 1, "\u2013"));         //$NON-NLS-1$
            }
        } else if (message.equals(EM_DASH_MESSAGE)) {
            int offset = text.indexOf("--");                             //$NON-NLS-1$
            if (offset != -1) {
                edits.add(new ReplaceEdit(offset, 2, "\u2014"));         //$NON-NLS-1$
            }
        } else if (message.equals(TYPOGRAPHIC_APOSTROPHE_MESSAGE)) {
            int offset = text.indexOf('\'');
            if (offset != -1) {
                edits.add(new ReplaceEdit(offset, 1, "\u2019"));         //$NON-NLS-1$
            }
        } else if (message.equals(COPYRIGHT_MESSAGE)) {
            int offset = text.indexOf("(c)");                            //$NON-NLS-1$
            if (offset == -1) {
                offset = text.indexOf("(C)");                            //$NON-NLS-1$
            }
            if (offset != -1) {
                edits.add(new ReplaceEdit(offset, 3, "\u00A9"));         //$NON-NLS-1$
            }
        } else if (message.equals(SINGLE_QUOTE_MESSAGE)) {
            int offset = text.indexOf('\'');
            if (offset != -1) {
                int endOffset = text.indexOf('\'', offset + 1);           //$NON-NLS-1$
                if (endOffset != -1) {
                    edits.add(new ReplaceEdit(offset, 1, "\u2018"));     //$NON-NLS-1$
                    edits.add(new ReplaceEdit(endOffset, 1, "\u2019"));  //$NON-NLS-1$
                }
            }
        } else if (message.equals(DBL_QUOTES_MESSAGE)) {
            int offset = text.indexOf('"');
            if (offset != -1) {
                int endOffset = text.indexOf('"', offset + 1);
                if (endOffset != -1) {
                    edits.add(new ReplaceEdit(offset, 1, "\u201C"));     //$NON-NLS-1$
                    edits.add(new ReplaceEdit(endOffset, 1, "\u201D"));  //$NON-NLS-1$
                }
            }
        } else if (message.equals(GRAVE_QUOTE_MESSAGE)) {
            int offset = text.indexOf('`');
            if (offset != -1) {
                int endOffset = text.indexOf('\'', offset + 1);
                if (endOffset != -1) {
                    edits.add(new ReplaceEdit(offset, 1, "\u2018"));     //$NON-NLS-1$
                    edits.add(new ReplaceEdit(endOffset, 1, "\u2019"));  //$NON-NLS-1$
                }
            }
        } else {
            Matcher matcher = Pattern.compile(FRACTION_MESSAGE_PATTERN).matcher(message);
            if (matcher.find()) {
                //  "Use fraction character %1$c (%2$s) instead of %3$s ?";
                String replace = matcher.group(3);
                int offset = text.indexOf(replace);
                if (offset != -1) {
                    String replaceWith = matcher.group(2);
                    edits.add(new ReplaceEdit(offset, replace.length(), replaceWith));
                }
            }
        }

        return edits;
    }
}
