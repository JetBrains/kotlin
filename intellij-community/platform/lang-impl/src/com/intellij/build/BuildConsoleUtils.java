/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.build;

import com.intellij.build.events.Failure;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vladislav.Soroka
 */
public class BuildConsoleUtils {
  private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
  private static final Pattern A_PATTERN = Pattern.compile("<a ([^>]* )?href=[\"\']([^>]*)[\"\'][^>]*>");
  private static final String A_CLOSING = "</a>";
  private static final Set<String> NEW_LINES = ContainerUtil.set("<br>", "</br>", "<br/>", "<p>", "</p>", "<p/>", "<pre>", "</pre>");

  public static boolean printDetails(ConsoleView consoleView, Failure failure) {
    return printDetails(consoleView, failure, null);
  }

  public static boolean printDetails(ConsoleView consoleView, @Nullable Failure failure, @Nullable String details) {
    String text = failure == null ? details : ObjectUtils.chooseNotNull(failure.getDescription(), failure.getMessage());
    if (text == null && failure != null && failure.getError() != null) {
      text = failure.getError().getMessage();
    }
    if (text == null) return false;

    String content = StringUtil.convertLineSeparators(text);
    while (true) {
      Matcher tagMatcher = TAG_PATTERN.matcher(content);
      if (!tagMatcher.find()) {
        consoleView.print(content, ConsoleViewContentType.ERROR_OUTPUT);
        break;
      }
      String tagStart = tagMatcher.group();
      consoleView.print(content.substring(0, tagMatcher.start()), ConsoleViewContentType.ERROR_OUTPUT);
      Matcher aMatcher = A_PATTERN.matcher(tagStart);
      if (aMatcher.matches()) {
        final String href = aMatcher.group(2);
        int linkEnd = content.indexOf(A_CLOSING, tagMatcher.end());
        if (linkEnd > 0) {
          String linkText = content.substring(tagMatcher.end(), linkEnd).replaceAll(TAG_PATTERN.pattern(), "");
          consoleView.printHyperlink(linkText, new HyperlinkInfo() {
            @Override
            public void navigate(Project project) {
              if(failure == null) {
                return;
              }
              Notification notification = failure.getNotification();
              if (notification != null && notification.getListener() != null) {
                notification.getListener().hyperlinkUpdate(
                  notification, IJSwingUtilities.createHyperlinkEvent(href, consoleView.getComponent()));
              }
            }
          });
          content = content.substring(linkEnd + A_CLOSING.length());
          continue;
        }
      }
      if (NEW_LINES.contains(tagStart)) {
        consoleView.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
      }
      else {
        consoleView.print(content.substring(tagMatcher.start(), tagMatcher.end()), ConsoleViewContentType.ERROR_OUTPUT);
      }
      content = content.substring(tagMatcher.end());
    }

    consoleView.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    return true;
  }
}
