/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.integrations.maven.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.GroovyFileType;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class AddGradleDslDependencyAction extends CodeInsightAction {
  static final ThreadLocal<List<MavenId>> TEST_THREAD_LOCAL = new ThreadLocal<>();

  public AddGradleDslDependencyAction() {
    getTemplatePresentation().setDescription(GradleBundle.message("gradle.codeInsight.action.add_maven_dependency.description"));
    getTemplatePresentation().setText(GradleBundle.message("gradle.codeInsight.action.add_maven_dependency.text"));
    getTemplatePresentation().setIcon(AllIcons.Nodes.PpLib);
  }

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new AddGradleDslDependencyActionHandler();
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (file instanceof PsiCompiledElement) return false;
    if (!GroovyFileType.GROOVY_FILE_TYPE.equals(file.getFileType())) return false;
    return !GradleConstants.SETTINGS_FILE_NAME.equals(file.getName()) && file.getName().endsWith(GradleConstants.EXTENSION);
  }
}
