// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utility methods for building documentation preview.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public final class DocPreviewUtil {

  private static final TIntHashSet ALLOWED_LINK_SEPARATORS = new TIntHashSet();
  static {
    ALLOWED_LINK_SEPARATORS.add(',');
    ALLOWED_LINK_SEPARATORS.add(' ');
    ALLOWED_LINK_SEPARATORS.add('.');
    ALLOWED_LINK_SEPARATORS.add(';');
    ALLOWED_LINK_SEPARATORS.add('&');
    ALLOWED_LINK_SEPARATORS.add('\t');
    ALLOWED_LINK_SEPARATORS.add('\n');
    ALLOWED_LINK_SEPARATORS.add('[');
    ALLOWED_LINK_SEPARATORS.add(']');
    ALLOWED_LINK_SEPARATORS.add('(');
    ALLOWED_LINK_SEPARATORS.add(')');
    ALLOWED_LINK_SEPARATORS.add('<');
    ALLOWED_LINK_SEPARATORS.add('>');
  }

  /**
   * We shorten links text from fully qualified name to short names (e.g. from {@code 'java.lang.String'} to {@code 'String'}).
   * There is a possible situation then that we have two replacements where one key is a simple name and another one is a fully qualified
   * one. We want to apply {@code 'from fully qualified name'} replacement first then.
   */
  private static final Comparator<String> REPLACEMENTS_COMPARATOR = new Comparator<String>() {
    @Override
    public int compare(@NotNull String o1, @NotNull String o2) {
      String shortName1 = extractShortName(o1);
      String shortName2 = extractShortName(o2);
      if (!shortName1.equals(shortName2)) {
        return shortName1.compareTo(shortName2);
      }
      if (o1.endsWith(o2)) {
        return -1;
      }
      else if (o2.endsWith(o1)) {
        return 1;
      }
      else {
        return o1.compareTo(o2);
      }
    }

    private String extractShortName(@NotNull String s) {
      int i = s.lastIndexOf('.');
      return i > 0 && i < s.length() - 1 ? s.substring(i + 1) : s;
    }
  };

  private DocPreviewUtil() {
  }

  /**
   * Allows to build a documentation preview from the given arguments. Basically, takes given 'header' text and tries to modify
   * it by using hyperlink information encapsulated at the given 'full text'.
   *
   * @param header                     target documentation header. Is expected to be a result of the
   *                                   {@link DocumentationProvider#getQuickNavigateInfo(PsiElement, PsiElement)} call
   * @param qName                      there is a possible case that not all documentation text will be included to the preview
   *                                   (according to the given 'desired rows and columns per-row' arguments). A link that points to the
   *                                   element with the given qualified name is added to the preview's end if the qName is provided then
   * @param fullText                   full documentation text (if available)
   */
  @NotNull
  public static String buildPreview(@NotNull final String header, @Nullable final String qName, @Nullable final String fullText) {
    if (fullText == null) {
      return header;
    }

    // Build links info.
    Map<String/*qName*/, String/*address*/> links = new HashMap<>();
    process(fullText, new LinksCollector(links));

    // Add derived names.
    Map<String, String> toAdd = new HashMap<>();
    for (Map.Entry<String, String> entry : links.entrySet()) {
      String shortName = parseShortName(entry.getKey());
      if (shortName != null) {
        toAdd.put(shortName, entry.getValue());
      }
      String longName = parseLongName(entry.getKey(), entry.getValue());
      if (longName != null) {
        toAdd.put(longName, entry.getValue());
      }
    }
    links.putAll(toAdd);
    if (qName != null) {
      links.put(qName, DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL + qName);
    }

    // Apply links info to the header template.
    List<TextRange> modifiedRanges = new ArrayList<>();
    List<String> sortedReplacements = new ArrayList<>(links.keySet());
    sortedReplacements.sort(REPLACEMENTS_COMPARATOR);
    StringBuilder buffer = new StringBuilder(header);
    replace(buffer, "\n", "<br/>", modifiedRanges);
    for (String replaceFrom : sortedReplacements) {
      replace(buffer, replaceFrom, String.format("<a href=\"%s\">%s</a>", links.get(replaceFrom), replaceFrom), modifiedRanges);
    }
    return buffer.toString();
  }

  /**
   * Tries to build a short name form the given name assuming that it is a full name.
   * <p/>
   * Example: return {@code 'String'} for a given {@code 'java.lang.String'}.
   *
   * @param name  name to process
   * @return      short name derived from the given full name if possible; {@code null} otherwise
   */
  @Nullable
  private static String parseShortName(@NotNull String name) {
    int i = name.lastIndexOf('.');
    return i > 0 && i < name.length() - 1 ? name.substring(i + 1) : null;
  }

  /**
   * Tries to build a long name from the given short name and a link.
   * <p/>
   * Example: return {@code 'java.lang.String'} for a given pair (name {@code 'String'}; address: {@code 'psi_element://java.lang.String'}.
   *
   * @param shortName   short name to process
   * @param address     address to process
   * @return            long name derived from the given arguments (if any); {@code null} otherwise
   */
  @Nullable
  private static String parseLongName(@NotNull String shortName, @NotNull String address) {
    String pureAddress = address;
    int i = pureAddress.lastIndexOf("//");
    if (i > 0 && i < pureAddress.length() - 2) {
      pureAddress = pureAddress.substring(i + 2);
    }

    return (pureAddress.equals(shortName) || !pureAddress.endsWith(shortName)) ? null : pureAddress;
  }

  private static void replace(@NotNull StringBuilder text,
                              @NotNull String replaceFrom,
                              @NotNull String replaceTo,
                              @NotNull List<TextRange> readOnlyChanges) {

    if (replaceFrom.isEmpty()) {
      return;
    }

    for (int i = text.indexOf(replaceFrom); i >= 0; i = text.indexOf(replaceFrom, i + 1)) {
      int end = i + replaceFrom.length();
      if (intersects(readOnlyChanges, i, end)) {
        continue;
      }
      if (!"\n".equals(replaceFrom)) {
        if (end < text.length() && !ALLOWED_LINK_SEPARATORS.contains(text.charAt(end))) {
          // Consider a situation when we have, say, replacement from text 'PsiType' and encounter a 'PsiTypeParameter' in the text.
          // We don't want to perform the replacement then.
          continue;
        }
        if (i > 0 && !ALLOWED_LINK_SEPARATORS.contains(text.charAt(i - 1))) {
          // Similar situation but targets head match: from = 'TextRange', text = 'getTextRange()'.
          continue;
        }
      }
      text.replace(i, end, replaceTo);
      int diff = replaceTo.length() - replaceFrom.length();
      for (int j = 0; j < readOnlyChanges.size(); j++) {
        TextRange range = readOnlyChanges.get(j);
        if (range.getStartOffset() >= end) {
          readOnlyChanges.set(j, range.shiftRight(diff));
        }
      }
      readOnlyChanges.add(new TextRange(i, i + replaceTo.length()));
    }
  }

  private static boolean intersects(@NotNull List<? extends TextRange> ranges, int start, int end) {
    for (TextRange range : ranges) {
      if (range.intersectsStrict(start, end)) {
        return true;
      }
    }
    return false;
  }

  private enum State {TEXT, INSIDE_OPEN_TAG, INSIDE_CLOSE_TAG}

  private static int process(@NotNull String text, @NotNull Callback callback) {
    State state = State.TEXT;
    int dataStartOffset = 0;
    int tagNameStartOffset = 0;
    String tagName = null;
    int i = 0;
    for (; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (state) {
        case TEXT:
          if (c == '<') {
            if (i > dataStartOffset) {
              if (!callback.onText(text.substring(dataStartOffset, i).replace("&nbsp;", " "))) {
                return dataStartOffset;
              }
            }
            dataStartOffset = i;
            if (i < text.length() - 1 && text.charAt(i + 1) == '/') {
              state = State.INSIDE_CLOSE_TAG;
              tagNameStartOffset = ++i + 1;
            }
            else {
              state = State.INSIDE_OPEN_TAG;
              tagNameStartOffset = i + 1;
            }
          }
          break;
        case INSIDE_OPEN_TAG:
          if (c == ' ') {
            tagName = text.substring(tagNameStartOffset, i);
          }
          else if (c == '/') {
            if (i < text.length() - 1 && text.charAt(i + 1) == '>') {
              if (tagName == null) {
                tagName = text.substring(tagNameStartOffset, i);
              }
              if (!callback.onStandaloneTag(tagName, text.substring(dataStartOffset, i + 2))) {
                return dataStartOffset;
              }
              tagName = null;
              state = State.TEXT;
              dataStartOffset = ++i + 1;
              break;
            }
          }
          else if (c == '>') {
            if (tagName == null) {
              tagName = text.substring(tagNameStartOffset, i);
            }
            if (!callback.onOpenTag(tagName, text.substring(dataStartOffset, i + 1))) {
              return dataStartOffset;
            }
            tagName = null;
            state = State.TEXT;
            dataStartOffset = i + 1;
          }
          break;
        case INSIDE_CLOSE_TAG:
          if (c == '>') {
            if (tagName == null) {
              tagName = text.substring(tagNameStartOffset, i);
            }
            if (!callback.onCloseTag(tagName, text.substring(dataStartOffset, i + 1))) {
              return dataStartOffset;
            }
            tagName = null;
            state = State.TEXT;
            dataStartOffset = i + 1;
          }
      }
    }

    if (dataStartOffset < text.length()) {
      callback.onText(text.substring(dataStartOffset).replace("&nbsp;", " "));
    }

    return i;
  }

  private interface Callback {
    boolean onOpenTag(@NotNull String name, @NotNull String text);
    boolean onCloseTag(@NotNull String name, @NotNull String text);
    boolean onStandaloneTag(@NotNull String name, @NotNull String text);
    boolean onText(@NotNull String text);
  }

  private static class LinksCollector implements Callback {

    private static final Pattern HREF_PATTERN = Pattern.compile("href=[\"']([^\"']+)");

    @NotNull private final Map<String, String> myLinks;
    private                String              myHref;

    LinksCollector(@NotNull Map<String, String> links) {
      myLinks = links;
    }

    @Override
    public boolean onOpenTag(@NotNull String name, @NotNull String text) {
      if (!"a".equals(name)) {
        return true;
      }
      Matcher matcher = HREF_PATTERN.matcher(text);
      if (matcher.find()) {
        myHref = matcher.group(1);
      }
      return true;
    }

    @Override
    public boolean onCloseTag(@NotNull String name, @NotNull String text) {
      if ("a".equals(name)) {
        myHref = null;
      }
      return true;
    }

    @Override
    public boolean onStandaloneTag(@NotNull String name, @NotNull String text) {
      return true;
    }

    @Override
    public boolean onText(@NotNull String text) {
      if (myHref != null) {
        myLinks.put(text, myHref);
        myHref = null;
      }
      return true;
    }
  }
}
