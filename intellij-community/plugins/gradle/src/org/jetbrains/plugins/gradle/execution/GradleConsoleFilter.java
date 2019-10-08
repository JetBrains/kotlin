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
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vladislav.Soroka
 */
public class GradleConsoleFilter implements Filter {
  public static final Pattern LINE_AND_COLUMN_PATTERN = Pattern.compile("line (\\d+), column (\\d+)\\.");

  @Nullable
  private final Project myProject;
  private static final TextAttributes HYPERLINK_ATTRIBUTES =
    EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);
  private String myFilteredFileName;
  private int myFilteredLineNumber;

  public GradleConsoleFilter(@Nullable Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public Result applyFilter(@NotNull final String line, final int entireLength) {
    String[] filePrefixes = new String[]{"Build file '", "build file '", "Settings file '", "settings file '"};
    String[] linePrefixes = new String[]{"' line: ", "': ", "' line: ", "': "};
    String filePrefix = null;
    String linePrefix = null;
    for (int i = 0; i < filePrefixes.length; i++) {
      int filePrefixIndex = StringUtil.indexOf(line, filePrefixes[i]);
      if (filePrefixIndex != -1) {
        filePrefix = filePrefixes[i];
        linePrefix = linePrefixes[i];
        break;
      }
    }

    if (filePrefix == null || linePrefix == null) {
      return null;
    }

    int filePrefixIndex = StringUtil.indexOf(line, filePrefix);

    final String fileAndLineNumber = line.substring(filePrefix.length() + filePrefixIndex);
    int linePrefixIndex = StringUtil.indexOf(fileAndLineNumber, linePrefix);

    if (linePrefixIndex == -1) {
      return null;
    }

    final String fileName = fileAndLineNumber.substring(0, linePrefixIndex);
    myFilteredFileName = fileName;
    String lineNumberStr = fileAndLineNumber.substring(linePrefixIndex + linePrefix.length()).trim();
    int lineNumberEndIndex = 0;
    for (int i = 0; i < lineNumberStr.length(); i++) {
      if (Character.isDigit(lineNumberStr.charAt(i))) {
        lineNumberEndIndex = i;
      }
      else {
        break;
      }
    }
    lineNumberStr = lineNumberStr.substring(0, lineNumberEndIndex + 1);
    int lineNumber;
    try {
      lineNumber = Integer.parseInt(lineNumberStr);
      myFilteredLineNumber = lineNumber;
    }
    catch (NumberFormatException e) {
      return null;
    }

    final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(fileName.replace(File.separatorChar, '/'));
    if (file == null) {
      return null;
    }

    int textStartOffset = entireLength - line.length() + filePrefix.length() + filePrefixIndex;
    int highlightEndOffset = textStartOffset + fileName.length();
    OpenFileHyperlinkInfo info = null;
    if (myProject != null) {
      int columnNumber = 0;
      String lineAndColumn = StringUtil.substringAfterLast(line, " @ ");
      if (lineAndColumn != null) {
        Matcher matcher = LINE_AND_COLUMN_PATTERN.matcher(lineAndColumn);
        if (matcher.find()) {
          columnNumber = Integer.parseInt(matcher.group(2));
        }
      }
      info = new OpenFileHyperlinkInfo(myProject, file, Math.max(lineNumber - 1, 0), columnNumber);
    }
    TextAttributes attributes = HYPERLINK_ATTRIBUTES.clone();
    if (myProject != null && !ProjectRootManager.getInstance(myProject).getFileIndex().isInContent(file)) {
      Color color = UIUtil.getInactiveTextColor();
      attributes.setForegroundColor(color);
      attributes.setEffectColor(color);
    }
    return new Result(textStartOffset, highlightEndOffset, info, attributes);
  }

  public String getFilteredFileName() {
    return myFilteredFileName;
  }

  public int getFilteredLineNumber() {
    return myFilteredLineNumber;
  }
}
