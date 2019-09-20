// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiFile;
import icons.GradleIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.codeInsight.GradlePluginDescriptionsExtension;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.GroovyFileType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.util.Pair.pair;

/**
 * @author Vladislav.Soroka
 */
public class AddGradleDslPluginAction extends CodeInsightAction {
  public static final String ID = "AddGradleDslPluginAction";
  static final ThreadLocal<String> TEST_THREAD_LOCAL = new ThreadLocal<>();
  private final List<Pair<String, String>> myPlugins;

  public AddGradleDslPluginAction() {
    getTemplatePresentation().setDescription(GradleBundle.message("gradle.codeInsight.action.apply_plugin.description"));
    getTemplatePresentation().setText(GradleBundle.message("gradle.codeInsight.action.apply_plugin.text"));
    getTemplatePresentation().setIcon(GradleIcons.GradlePlugin);

    myPlugins = new ArrayList<>();
    for (GradlePluginDescriptionsExtension extension : GradlePluginDescriptionsExtension.EP_NAME.getExtensions()) {
      for (Map.Entry<String, String> pluginDescription : extension.getPluginDescriptions().entrySet()) {
        myPlugins.add(pair(pluginDescription.getKey(), pluginDescription.getValue()));
      }
    }
    myPlugins.sort(Comparator.comparing(o -> o.first));
  }

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new AddGradleDslPluginActionHandler(myPlugins);
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (file instanceof PsiCompiledElement) return false;
    if (!GroovyFileType.GROOVY_FILE_TYPE.equals(file.getFileType())) return false;
    return !GradleConstants.SETTINGS_FILE_NAME.equals(file.getName()) && file.getName().endsWith(GradleConstants.EXTENSION);
  }
}