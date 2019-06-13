// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.statusbar;

import com.intellij.application.options.CodeStyleConfigurableWrapper;
import com.intellij.application.options.CodeStyleSchemesConfigurable;
import com.intellij.application.options.codeStyle.OtherFileTypesCodeStyleConfigurable;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CodeStyleStatusBarWidgetProvider implements StatusBarWidgetProvider {
  @Nullable
  @Override
  public StatusBarWidget getWidget(@NotNull Project project) {
    return new CodeStyleStatusBarWidget(project);
  }

  @NotNull
  @Override
  public String getAnchor() {
    return StatusBar.Anchors.after(StatusBar.StandardWidgets.ENCODING_PANEL);
  }

  @NotNull
  public static DumbAwareAction createDefaultIndentConfigureAction(@NotNull PsiFile psiFile) {
    return DumbAwareAction.create(
      ApplicationBundle.message("code.style.widget.configure.indents", psiFile.getLanguage().getDisplayName()),
      event -> {
        Configurable configurable = findCodeStyleConfigurableId(psiFile);
        if (configurable instanceof CodeStyleConfigurableWrapper) {
          ShowSettingsUtil.getInstance().editConfigurable(event.getProject(), configurable,
                                                          () -> ((CodeStyleConfigurableWrapper)configurable)
                                                            .selectTab(ApplicationBundle.message("title.tabs.and.indents")));
        }
      }
    );
  }

  @Nullable
  public static Configurable findCodeStyleConfigurableId(@NotNull PsiFile file) {
    final Project project = file.getProject();
    final Language language = file.getLanguage();
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.findUsingBaseLanguage(language);
    CodeStyleSchemesConfigurable topConfigurable = new CodeStyleSchemesConfigurable(project);
    if (provider != null && provider.getIndentOptionsEditor() != null) {
      String name = provider.getConfigurableDisplayName();
      if (name != null) {
        SearchableConfigurable result = topConfigurable.findSubConfigurable(name);
        if (result != null) {
          return result;
        }
      }
    }
    return topConfigurable.findSubConfigurable(OtherFileTypesCodeStyleConfigurable.DISPLAY_NAME);
  }
}
