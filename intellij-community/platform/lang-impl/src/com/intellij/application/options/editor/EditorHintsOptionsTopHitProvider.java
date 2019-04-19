// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.ide.ui.EditorOptionsTopHitProvider;
import com.intellij.ide.ui.OptionsTopHitProvider;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

import static com.intellij.ide.ui.OptionsTopHitProvider.messageApp;

public class EditorHintsOptionsTopHitProvider implements OptionsTopHitProvider.ApplicationLevelProvider {
  private final Collection<OptionDescription> myOptions = Collections.singleton(
    new BooleanOptionDescription("Appearance: " + messageApp("checkbox.show.parameter.name.hints"), "editor.preferences.appearance") {
      @Override
      public boolean isOptionEnabled() {
        return EditorSettingsExternalizable.getInstance().isShowParameterNameHints();
      }

      @Override
      public void setOptionState(boolean enabled) {
        EditorSettingsExternalizable.getInstance().setShowParameterNameHints(enabled);
        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
          DaemonCodeAnalyzer.getInstance(project).restart();
        }
      }
    });

  @NotNull
  @Override
  public Collection<OptionDescription> getOptions() {
    return myOptions;
  }

  @NotNull
  @Override
  public String getId() {
    return EditorOptionsTopHitProvider.ID;
  }
}
