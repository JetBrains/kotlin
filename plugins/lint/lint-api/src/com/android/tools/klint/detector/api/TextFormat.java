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

package com.android.tools.lint.detector.api;

import com.android.annotations.NonNull;
import com.android.utils.SdkUtils;
import com.android.utils.XmlUtils;

/**
 * Lint error message, issue explanations and location descriptions
 * are described in a {@link #RAW} format which looks similar to text
 * but which can contain bold, symbols and links. These issues can
 * also be converted to plain text and to HTML markup, using the
 * {@link #convertTo(String, TextFormat)} method.
 *
 * @see Issue#getDescription(TextFormat)
 * @see Issue#getExplanation(TextFormat)
 * @see Issue#getBriefDescription(TextFormat)
 */
public enum TextFormat {
    /**
     * Raw output format which is similar to text but allows some markup:
     * <ul>
     * <li>HTTP urls (http://...)
     * <li>Sentences immediately surrounded by * will be shown as bold.
     * <li>Sentences immediately surrounded by ` will be shown using monospace
     * fonts
     * </ul>
     * Furthermore, newlines are converted to br's when converting newlines.
     * Note: It does not insert {@code <html>} tags around the fragment for HTML output.
     * <p>
     * TODO: Consider switching to the restructured text format -
     *  http://docutils.sourceforge.net/docs/user/rst/quickstart.html
     */
    RAW,

    /**
     * Plain text output
     */
    TEXT,

    /**
     * HTML formatted output (note: does not include surrounding {@code <html></html>} tags)
     */
    HTML;

    /**
     * Converts the given text to HTML
     *
     * @param text the text to format
     * @return the corresponding text formatted as HTML
     */
    @NonNull
    public String toHtml(@NonNull String text) {
        return convertTo(text, HTML);
    }

    /**
     * Converts the given text to plain text
     *
     * @param text the tetx to format
     * @return the corresponding text formatted as HTML
     */
    @NonNull
    public String toText(@NonNull String text) {
        return convertTo(text, TEXT);
    }

    /**
     * Converts the given message to the given format. Note that some
     * conversions are lossy; e.g. once converting away from the raw format
     * (which contains all the markup) you can't convert back to it.
     * Note that you can convert to the format it's already in; that just
     * returns the same string.
     *
     * @param message the message to convert
     * @param to the format to convert to
     * @return a converted message
     */
    public String convertTo(@NonNull String message, @NonNull TextFormat to) {
        if (this == to) {
            return message;
        }
        switch (this) {
            case RAW: {
                switch (to) {
                    case RAW:
                        return message;
                    case TEXT:
                    case HTML:
                        return to.fromRaw(message);
                }
            }
            case TEXT: {
                switch (to) {
                    case TEXT:
                    case RAW:
                        return message;
                    case HTML:
                        return XmlUtils.toXmlTextValue(message);
                }
            }
            case HTML: {
                switch (to) {
                    case HTML:
                        return message;
                    case RAW:
                    case TEXT: {
                        return to.fromHtml(message);

                    }
                }
            }
        }
        return message;
    }

    /** Converts to this output format from the given HTML-format text */
    @NonNull
    private String fromHtml(@NonNull String html) {
        assert this == RAW || this == TEXT : this;

        // Drop all tags; replace all entities, insert newlines
        // (this won't do wrapping)
        StringBuilder sb = new StringBuilder(html.length());
        for (int i = 0, n = html.length(); i < n; i++) {
            char c = html.charAt(i);
            if (c == '<') {
                // Scan forward to the end
                if (html.startsWith("<br>", i) ||
                        html.startsWith("<br />", i) ||
                        html.startsWith("<BR>", i) ||
                        html.startsWith("<BR />", i)) {
                    sb.append('\n');
                } else if (html.startsWith("<!--")) {
                    i = Math.max(i, html.indexOf("-->", i));
                }
                i = html.indexOf('>', i);
            } else if (c == '&') {
                int end = html.indexOf(';', i);
                if (end > i) {
                    String entity = html.substring(i, end + 1);
                    sb.append(XmlUtils.fromXmlAttributeValue(entity));
                    i = end;
                } else {
                    sb.append(c);
                }
            } else if (c == '\n') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }

        // Collapse repeated spaces
        String s = sb.toString();
        sb.setLength(0);
        boolean wasSpace = false;
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            if (c == '\t') { // we keep newlines; came from <br>'s
                c = ' ';
            }
            boolean isSpace = c == ' ';
            if (!isSpace || !wasSpace) {
                wasSpace = isSpace;
                sb.append(c);
            }
        }
        s = sb.toString();

        // Line-wrap
        s = SdkUtils.wrap(s, 60, null);

        return s;
    }

    private static final String HTTP_PREFIX = "http://"; //$NON-NLS-1$

    /** Converts to this output format from the given raw-format text */
    @NonNull
    private String fromRaw(@NonNull String text) {
        assert this == HTML || this == TEXT : this;
        StringBuilder sb = new StringBuilder(3 * text.length() / 2);
        boolean html = this == HTML;

        char prev = 0;
        int flushIndex = 0;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if ((c == '*' || c == '`' && i < n - 1)) {
                // Scout ahead for range end
                if (!Character.isLetterOrDigit(prev)
                        && !Character.isWhitespace(text.charAt(i + 1))) {
                    // Found * or ` immediately before a letter, and not in the middle of a word
                    // Find end
                    int end = text.indexOf(c, i + 1);
                    if (end != -1 && (end == n - 1 || !Character.isLetter(text.charAt(end + 1)))) {
                        if (i > flushIndex) {
                            appendEscapedText(sb, text, html, flushIndex, i);
                        }
                        if (html) {
                            String tag = c == '*' ? "b" : "code"; //$NON-NLS-1$ //$NON-NLS-2$
                            sb.append('<').append(tag).append('>');
                            appendEscapedText(sb, text, html, i + 1, end);
                            sb.append('<').append('/').append(tag).append('>');
                        } else {
                            appendEscapedText(sb, text, html, i + 1, end);
                        }
                        flushIndex = end + 1;
                        i = flushIndex - 1; // -1: account for the i++ in the loop
                    }
                }
            } else if (html && c == 'h' && i < n - 1 && text.charAt(i + 1) == 't'
                    && text.startsWith(HTTP_PREFIX, i) && !Character.isLetterOrDigit(prev)) {
                // Find url end
                int end = i + HTTP_PREFIX.length();
                while (end < n) {
                    char d = text.charAt(end);
                    if (Character.isWhitespace(d)) {
                        break;
                    }
                    end++;
                }
                char last = text.charAt(end - 1);
                if (last == '.' || last == ')' || last == '!') {
                    end--;
                }
                if (end > i + HTTP_PREFIX.length()) {
                    if (i > flushIndex) {
                        appendEscapedText(sb, text, html, flushIndex, i);
                    }

                    String url = text.substring(i, end);
                    sb.append("<a href=\"");        //$NON-NLS-1$
                    sb.append(url);
                    sb.append('"').append('>');
                    sb.append(url);
                    sb.append("</a>");              //$NON-NLS-1$

                    flushIndex = end;
                    i = flushIndex - 1; // -1: account for the i++ in the loop
                }
            }
            prev = c;
        }

        if (flushIndex < n) {
            appendEscapedText(sb, text, html, flushIndex, n);
        }

        return sb.toString();
    }

    private static void appendEscapedText(@NonNull StringBuilder sb, @NonNull String text,
            boolean html, int start, int end) {
        if (html) {
            for (int i = start; i < end; i++) {
                char c = text.charAt(i);
                if (c == '<') {
                    sb.append("&lt;");                                   //$NON-NLS-1$
                } else if (c == '&') {
                    sb.append("&amp;");                                  //$NON-NLS-1$
                } else if (c == '\n') {
                    sb.append("<br/>\n");
                } else {
                    if (c > 255) {
                        sb.append("&#");                                 //$NON-NLS-1$
                        sb.append(Integer.toString(c));
                        sb.append(';');
                    } else if (c == '\u00a0') {
                        sb.append("&nbsp;");                             //$NON-NLS-1$
                    } else {
                        sb.append(c);
                    }
                }
            }
        } else {
            for (int i = start; i < end; i++) {
                char c = text.charAt(i);
                sb.append(c);
            }
        }
    }
}
