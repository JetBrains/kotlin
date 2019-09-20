// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;


import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateEditor;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface PostfixTemplateProvider {
  /**
   * Identifier of template provider. Used for storing settings of provider's templates.
   */
  @NotNull
  default String getId() {
    return getClass().getName();
  }

  /**
   * Presentation name of editable template type. If null, provider doesn't allow to custom templates.
   */
  @Nullable
  default String getPresentableName() {
    return null;
  }

  /**
   * Returns builtin templates registered in the provider in their original state.
   * Consider using {@link PostfixTemplatesUtils#getAvailableTemplates(PostfixTemplateProvider)} for actually enabled templates.
   */
  @NotNull
  Set<PostfixTemplate> getTemplates();

  /**
   * Check symbol can separate template keys
   */
  boolean isTerminalSymbol(char currentChar);

  /**
   * Prepare file for template expanding. Running on EDT.
   * E.g. java postfix templates adds semicolon after caret in order to simplify context checking.
   * <p>
   * File content doesn't contain template's key, it is deleted just before this method invocation.
   * <p>
   * Note that while postfix template is checking its availability the file parameter is a _COPY_ of the real file,
   * so you can do with it anything that you want, but in the same time it doesn't recommended to modify editor state because it's real.
   */
  void preExpand(@NotNull PsiFile file, @NotNull Editor editor);

  /**
   * Invoked after template finished (doesn't matter if it finished successfully or not).
   * E.g. java postfix template use this method for deleting inserted semicolon.
   */
  void afterExpand(@NotNull PsiFile file, @NotNull Editor editor);

  /**
   * Prepare file for checking availability of templates.
   * Almost the same as {@link this#preExpand(PsiFile, Editor)} with several differences:
   * 1. Processes copy of file. So implementations can modify it without corrupting the real file.
   * 2. Could be invoked from anywhere (EDT, write-action, read-action, completion-thread etc.). So implementations should make
   * additional effort to make changes in file.
   * <p>
   * Content of file copy doesn't contain template's key, it is deleted just before this method invocation.
   * <p>
   * NOTE: editor is real (not copy) and it doesn't represents the copyFile.
   * So it's safer to use currentOffset parameter instead of offset from editor. Do not modify text via editor.
   */
  @NotNull
  PsiFile preCheck(@NotNull PsiFile copyFile, @NotNull Editor realEditor, int currentOffset);

  /**
   * Return the editor that it able to represent template in UI
   * and create the template from the settings that users set in UI.
   * <p>
   * If templateToEdit is null, it's considered like an editor for a new template.
   */
  @Nullable
  default PostfixTemplateEditor createEditor(@Nullable PostfixTemplate templateToEdit) {
    return null;
  }

  /**
   * Instantiates the template that was serialized by the provider to XML.
   */
  @Nullable
  default PostfixTemplate readExternalTemplate(@NotNull String id, @NotNull String name, @NotNull Element template) {
    return null;
  }

  /**
   * Serialized to XML the template that was created by the provider.
   */
  default void writeExternalTemplate(@NotNull PostfixTemplate template, @NotNull Element parentElement) {
  }
}
