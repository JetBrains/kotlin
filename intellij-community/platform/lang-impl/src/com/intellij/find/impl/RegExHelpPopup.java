/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.find.impl;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.FindUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.util.MinimizeButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

/**
 * @author spleaner
 */
public class RegExHelpPopup extends JPanel {
  private final JEditorPane myEditorPane;
  private final JScrollPane myScrollPane;

  public RegExHelpPopup() {
    setLayout(new BorderLayout());

    myEditorPane = new JEditorPane();
    myEditorPane.setEditable(false);
    myEditorPane.setEditorKit(UIUtil.getHTMLEditorKit());
    myEditorPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myEditorPane.setBackground(HintUtil.getInformationColor());

    myEditorPane.setText("<html><h2> Summary of regular-expression constructs</h2> \n" +
                         " \n" +
                         " <table border=\"0\" cellpadding=\"1\" cellspacing=\"0\" \n" +
                         "  summary=\"Regular expression constructs, and what they match\"> \n" +
                         " \n" +
                         " <tr align=\"left\"> \n" +
                         " <th bgcolor=\"" +
                         ColorUtil.toHtmlColor(UIUtil.getLabelBackground()) +
                         "\" align=\"left\" id=\"construct\">Construct</th> \n" +
                         " <th bgcolor=\"" +
                         ColorUtil.toHtmlColor(UIUtil.getLabelBackground()) +
                         "\" align=\"left\" id=\"matches\">Matches</th> \n" +
                         " </tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"characters\">Characters</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><i>x</i></td> \n" +
                         "     <td headers=\"matches\">The character <i>x</i></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\\\</tt></td> \n" +
                         "     <td headers=\"matches\">The backslash character</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\0</tt><i>n</i></td> \n" +
                         "     <td headers=\"matches\">The character with octal value <tt>0</tt><i>n</i> \n" +
                         "         (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\0</tt><i>nn</i></td> \n" +
                         "     <td headers=\"matches\">The character with octal value <tt>0</tt><i>nn</i> \n" +
                         "         (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\0</tt><i>mnn</i></td> \n" +
                         "     <td headers=\"matches\">The character with octal value <tt>0</tt><i>mnn</i> \n" +
                         "         (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>m</i>&nbsp;<tt>&lt;=</tt>&nbsp;3,\n" +
                         "         0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\x</tt><i>hh</i></td> \n" +
                         "     <td headers=\"matches\">The character with hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>hh</i></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>&#92;u</tt><i>hhhh</i></td> \n" +
                         "     <td headers=\"matches\">The character with hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>hhhh</i></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"matches\"><tt>\\t</tt></td> \n" +
                         "     <td headers=\"matches\">The tab character (<tt>'&#92;u0009'</tt>)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\n</tt></td> \n" +
                         "     <td headers=\"matches\">The newline (line feed) character (<tt>'&#92;u000A'</tt>)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\r</tt></td> \n" +
                         "     <td headers=\"matches\">The carriage-return character (<tt>'&#92;u000D'</tt>)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\f</tt></td> \n" +
                         "     <td headers=\"matches\">The form-feed character (<tt>'&#92;u000C'</tt>)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\a</tt></td> \n" +
                         "     <td headers=\"matches\">The alert (bell) character (<tt>'&#92;u0007'</tt>)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\e</tt></td> \n" +
                         "     <td headers=\"matches\">The escape character (<tt>'&#92;u001B'</tt>)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\c</tt><i>x</i></td> \n" +
                         "     <td headers=\"matches\">The control character corresponding to <i>x</i></td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"classes\">Character classes</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct classes\"><tt>[abc]</tt></td> \n" +
                         "     <td headers=\"matches\"><tt>a</tt>, <tt>b</tt>, or <tt>c</tt> (simple class)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct classes\"><tt>[^abc]</tt></td> \n" +
                         "     <td headers=\"matches\">Any character except <tt>a</tt>, <tt>b</tt>, or <tt>c</tt> (negation)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-zA-Z]</tt></td> \n" +
                         "     <td headers=\"matches\"><tt>a</tt> through <tt>z</tt> \n" +
                         "         or <tt>A</tt> through <tt>Z</tt>, inclusive (range)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-d[m-p]]</tt></td> \n" +
                         "     <td headers=\"matches\"><tt>a</tt> through <tt>d</tt>,\n" +
                         "      or <tt>m</tt> through <tt>p</tt>: <tt>[a-dm-p]</tt> (union)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-z&&[def]]</tt></td> \n" +
                         "     <td headers=\"matches\"><tt>d</tt>, <tt>e</tt>, or <tt>f</tt> (intersection)</tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-z&&[^bc]]</tt></td> \n" +
                         "     <td headers=\"matches\"><tt>a</tt> through <tt>z</tt>,\n" +
                         "         except for <tt>b</tt> and <tt>c</tt>: <tt>[ad-z]</tt> (subtraction)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-z&&[^m-p]]</tt></td> \n" +
                         "     <td headers=\"matches\"><tt>a</tt> through <tt>z</tt>,\n" +
                         "          and not <tt>m</tt> through <tt>p</tt>: <tt>[a-lq-z]</tt>(subtraction)</td></tr> \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"predef\">Predefined character classes</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct predef\"><tt>.</tt></td> \n" +
                         "     <td headers=\"matches\">Any character (may or may not match line terminators)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\d</tt></td> \n" +
                         "     <td headers=\"matches\">A digit: <tt>[0-9]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\D</tt></td> \n" +
                         "     <td headers=\"matches\">A non-digit: <tt>[^0-9]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\s</tt></td> \n" +
                         "     <td headers=\"matches\">A whitespace character: <tt>[ \\t\\n\\x0B\\f\\r]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\S</tt></td> \n" +
                         "     <td headers=\"matches\">A non-whitespace character: <tt>[^\\s]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\w</tt></td> \n" +
                         "     <td headers=\"matches\">A word character: <tt>[a-zA-Z_0-9]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\W</tt></td> \n" +
                         "     <td headers=\"matches\">A non-word character: <tt>[^\\w]</tt></td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"posix\">POSIX character classes</b> (US-ASCII only)<b></th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Lower}</tt></td> \n" +
                         "     <td headers=\"matches\">A lower-case alphabetic character: <tt>[a-z]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Upper}</tt></td> \n" +
                         "     <td headers=\"matches\">An upper-case alphabetic character:<tt>[A-Z]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{ASCII}</tt></td> \n" +
                         "     <td headers=\"matches\">All ASCII:<tt>[\\x00-\\x7F]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Alpha}</tt></td> \n" +
                         "     <td headers=\"matches\">An alphabetic character:<tt>[\\p{Lower}\\p{Upper}]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Digit}</tt></td> \n" +
                         "     <td headers=\"matches\">A decimal digit: <tt>[0-9]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Alnum}</tt></td> \n" +
                         "     <td headers=\"matches\">An alphanumeric character:<tt>[\\p{Alpha}\\p{Digit}]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Punct}</tt></td> \n" +
                         "     <td headers=\"matches\">Punctuation: One of <tt>!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~</tt></td></tr> \n" +
                         "     <!-- <tt>[\\!\"#\\$%&'\\(\\)\\*\\+,\\-\\./:;\\<=\\>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~]</tt>\n" +
                         "          <tt>[\\X21-\\X2F\\X31-\\X40\\X5B-\\X60\\X7B-\\X7E]</tt> --> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Graph}</tt></td> \n" +
                         "     <td headers=\"matches\">A visible character: <tt>[\\p{Alnum}\\p{Punct}]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Print}</tt></td> \n" +
                         "     <td headers=\"matches\">A printable character: <tt>[\\p{Graph}\\x20]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Blank}</tt></td> \n" +
                         "     <td headers=\"matches\">A space or a tab: <tt>[ \\t]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Cntrl}</tt></td> \n" +
                         "     <td headers=\"matches\">A control character: <tt>[\\x00-\\x1F\\x7F]</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{XDigit}</tt></td> \n" +
                         "     <td headers=\"matches\">A hexadecimal digit: <tt>[0-9a-fA-F]</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Space}</tt></td> \n" +
                         "     <td headers=\"matches\">A whitespace character: <tt>[ \\t\\n\\x0B\\f\\r]</tt></td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\">java.lang.Character classes (simple java character type)</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\"><tt>\\p{javaLowerCase}</tt></td> \n" +
                         "     <td>Equivalent to java.lang.Character.isLowerCase()</td></tr> \n" +
                         " <tr><td valign=\"top\"><tt>\\p{javaUpperCase}</tt></td> \n" +
                         "     <td>Equivalent to java.lang.Character.isUpperCase()</td></tr> \n" +
                         " <tr><td valign=\"top\"><tt>\\p{javaWhitespace}</tt></td> \n" +
                         "     <td>Equivalent to java.lang.Character.isWhitespace()</td></tr> \n" +
                         " <tr><td valign=\"top\"><tt>\\p{javaMirrored}</tt></td> \n" +
                         "     <td>Equivalent to java.lang.Character.isMirrored()</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"unicode\">Classes for Unicode blocks and categories</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{InGreek}</tt></td> \n" +
                         "     <td headers=\"matches\">A character in the Greek&nbsp;block (simple block)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{Lu}</tt></td> \n" +
                         "     <td headers=\"matches\">An uppercase letter (simple category)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{Sc}</tt></td> \n" +
                         "     <td headers=\"matches\">A currency symbol</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\P{InGreek}</tt></td> \n" +
                         "     <td headers=\"matches\">Any character except one in the Greek block (negation)</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct unicode\"><tt>[\\p{L}&&[^\\p{Lu}]]&nbsp;</tt></td> \n" +
                         "     <td headers=\"matches\">Any letter except an uppercase letter (subtraction)</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"bounds\">Boundary matchers</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>^</tt></td> \n" +
                         "     <td headers=\"matches\">The beginning of a line</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>$</tt></td> \n" +
                         "     <td headers=\"matches\">The end of a line</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\b</tt></td> \n" +
                         "     <td headers=\"matches\">A word boundary</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\B</tt></td> \n" +
                         "     <td headers=\"matches\">A non-word boundary</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\A</tt></td> \n" +
                         "     <td headers=\"matches\">The beginning of the input</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\G</tt></td> \n" +
                         "     <td headers=\"matches\">The end of the previous match</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\Z</tt></td> \n" +
                         "     <td headers=\"matches\">The end of the input but for the final\n" +
                         "         terminator, if&nbsp;any</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\z</tt></td> \n" +
                         "     <td headers=\"matches\">The end of the input</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"greedy\">Greedy quantifiers</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>?</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, once or not at all</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>*</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, zero or more times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>+</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, one or more times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>{</tt><i>n</i><tt>}</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, exactly <i>n</i> times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>{</tt><i>n</i><tt>,}</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, at least <i>n</i> times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"reluc\">Reluctant quantifiers</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>??</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, once or not at all</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>*?</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, zero or more times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>+?</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, one or more times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>{</tt><i>n</i><tt>}?</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, exactly <i>n</i> times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>{</tt><i>n</i><tt>,}?</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, at least <i>n</i> times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}?</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"poss\">Possessive quantifiers</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>?+</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, once or not at all</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>*+</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, zero or more times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>++</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, one or more times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>{</tt><i>n</i><tt>}+</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, exactly <i>n</i> times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>{</tt><i>n</i><tt>,}+</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, at least <i>n</i> times</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}+</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"logical\">Logical operators</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct logical\"><i>XY</i></td> \n" +
                         "     <td headers=\"matches\"><i>X</i> followed by <i>Y</i></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct logical\"><i>X</i><tt>|</tt><i>Y</i></td> \n" +
                         "     <td headers=\"matches\">Either <i>X</i> or <i>Y</i></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct logical\"><tt>(</tt><i>X</i><tt>)</tt></td> \n" +
                         "     <td headers=\"matches\">X, as a capturing group</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"backref\">Back references</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"bottom\" headers=\"construct backref\"><tt>\\</tt><i>n</i></td> \n" +
                         "     <td valign=\"bottom\" headers=\"matches\">Whatever the <i>n</i><sup>th</sup> \n" +
                         "     capturing group matched</td></tr> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"quot\">Quotation</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct quot\"><tt>\\</tt></td> \n" +
                         "     <td headers=\"matches\">Nothing, but quotes the following character</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct quot\"><tt>\\Q</tt></td> \n" +
                         "     <td headers=\"matches\">Nothing, but quotes all characters until <tt>\\E</tt></td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct quot\"><tt>\\E</tt></td> \n" +
                         "     <td headers=\"matches\">Nothing, but ends quoting started by <tt>\\Q</tt></td></tr> \n" +
                         "     <!-- Metachars: !$()*+.<>?[\\]^{|} --> \n" +
                         " \n" +
                         " <tr><th>&nbsp;</th></tr> \n" +
                         " <tr align=\"left\"><th colspan=\"2\" id=\"special\">Special constructs (non-capturing)</th></tr> \n" +
                         " \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?:</tt><i>X</i><tt>)</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, as a non-capturing group</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?idmsux-idmsux)&nbsp;</tt></td> \n" +
                         "     <td headers=\"matches\">Nothing, but turns match flags on - off</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?idmsux-idmsux:</tt><i>X</i><tt>)</tt>&nbsp;&nbsp;</td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, as a non-capturing group with the\n" +
                         "         given flags on - off</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?=</tt><i>X</i><tt>)</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, via zero-width positive lookahead</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?!</tt><i>X</i><tt>)</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, via zero-width negative lookahead</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?&lt;=</tt><i>X</i><tt>)</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, via zero-width positive lookbehind</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?&lt;!</tt><i>X</i><tt>)</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, via zero-width negative lookbehind</td></tr> \n" +
                         " <tr><td valign=\"top\" headers=\"construct special\"><tt>(?&gt;</tt><i>X</i><tt>)</tt></td> \n" +
                         "     <td headers=\"matches\"><i>X</i>, as an independent, non-capturing group</td></tr> \n" +
                         " \n" +
                         " </table> \n" +
                         " <p>More on Regular Expressions: " +
                         "<a href=\"http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html\">Full Java Regular Expressions syntax description</a>, " +
                         "<a href=\"http://www.regular-expressions.info/java.html\">Using Regular Expressions in Java</a>." +
                         " </html>");


    myEditorPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) BrowserUtil.browse(e.getURL());
      }
    });

    myEditorPane.setCaretPosition(0);

    myScrollPane = ScrollPaneFactory.createScrollPane(myEditorPane);
    myScrollPane.setBorder(null);

    add(myScrollPane, BorderLayout.CENTER);
  }

  public static LinkLabel createRegExLink(@NotNull String title, @Nullable Component owner, @Nullable Logger logger) {
    return createRegExLink(title, owner, logger, null);
  }

  @NotNull
  public static LinkLabel createRegExLink(@NotNull String title, @Nullable Component owner, @Nullable Logger logger, @Nullable String place) {
    Runnable action = createRegExLinkRunnable(owner);
    return new LinkLabel<>(title, null, new LinkListener<Object>() {

      @Override
      public void linkSelected(LinkLabel aSource, Object aLinkData) {
        FindUtil.triggerRegexHelpClicked(place);
        action.run();
      }
    });
  }

  @NotNull
  public static Runnable createRegExLinkRunnable(@Nullable Component owner) {
    return new Runnable() {
      JBPopup helpPopup;

      @Override
      public void run() {
        if (helpPopup != null && !helpPopup.isDisposed() && helpPopup.isVisible()) {
          return;
        }
        RegExHelpPopup content = new RegExHelpPopup();
        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(content, content);
        helpPopup = builder
          .setCancelOnClickOutside(false)
          .setBelongsToGlobalPopupStack(true)
          .setFocusable(true)
          .setRequestFocus(true)
          .setMovable(true)
          .setResizable(true)
          .setCancelOnOtherWindowOpen(false).setCancelButton(new MinimizeButton("Hide"))
          .setTitle("Regular expressions syntax").setDimensionServiceKey(null, "RegExHelpPopup", true).createPopup();
        Disposer.register(helpPopup, new Disposable() {
          @Override
          public void dispose() {
            destroyPopup();
          }
        });
        if (owner != null) {
          helpPopup.showInCenterOf(owner);
        }
        else {
          helpPopup.showInFocusCenter();
        }
      }

      private void destroyPopup() {
        helpPopup = null;
      }
    };
  }

  @Override
  public Dimension getPreferredSize() {
    return JBUI.size(600, 300);
  }
}
