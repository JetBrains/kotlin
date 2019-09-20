// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.diff.impl.settings;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author oleg
 * Implement this interface to enable custom diff preview in Colors & Fonts Settings page
 */
public abstract class DiffPreviewProvider {
  public static final ExtensionPointName<DiffPreviewProvider> EP_NAME = ExtensionPointName.create("com.intellij.diffPreviewProvider");

  @NotNull
  public abstract DiffContent[] createContents();

  @NotNull
  public static DiffContent[] getContents() {
    // Assuming that standalone IDE should provide one provider
    final List<DiffPreviewProvider> providers = EP_NAME.getExtensionList();
    if (providers.size() != 0) {
      return providers.get(0).createContents();
    }
    return createContent(LEFT_TEXT, CENTER_TEXT, RIGHT_TEXT, StdFileTypes.JAVA);
  }

  @NotNull
  public static DiffContent[] createContent(@NotNull String left,
                                            @NotNull String center,
                                            @NotNull String right,
                                            @NotNull FileType fileType) {
    return new DiffContent[]{
      createContent(left, fileType),
      createContent(center, fileType),
      createContent(right, fileType)};
  }

  @NotNull
  private static DiffContent createContent(@NotNull String text, @NotNull FileType fileType) {
    return DiffContentFactory.getInstance().create(text, fileType);
  }

  @NonNls private static final String LEFT_TEXT = "class MyClass {\n" +
                                                  "  int value;\n" +
                                                  "\n" +
                                                  "  void leftOnly() {}\n" +
                                                  "\n" +
                                                  "  void foo() {\n" +
                                                  "   // Left changes\n" +
                                                  "  }\n" +
                                                  "\n" +
                                                  "  void bar() {\n" +
                                                  "\n" +
                                                  "  }\n" +
                                                  "}\n" +
                                                  "\n" +
                                                  "\n";
  @NonNls private static final String CENTER_TEXT = "class MyClass {\n" +
                                                    "  int value;\n" +
                                                    "\n" +
                                                    "  void foo() {\n" +
                                                    "  }\n" +
                                                    "\n" +
                                                    "  void removedFromLeft() {}\n" +
                                                    "\n" +
                                                    "  void bar() {\n" +
                                                    "\n" +
                                                    "  }\n" +
                                                    "}\n" +
                                                    "\n" +
                                                    "\n";
  @NonNls private static final String RIGHT_TEXT = "class MyClass {\n" +
                                                   "  long value;\n" +
                                                   "\n" +
                                                   "  void foo() {\n" +
                                                   "   // Right changes\n" +
                                                   "  }\n" +
                                                   "\n" +
                                                   "  void removedFromLeft() {}\n" +
                                                   "\n" +
                                                   "  void bar() {\n" +
                                                   "  }\n" +
                                                   "\n" +
                                                   "}\n" +
                                                   "\n" +
                                                   "\n";
}
