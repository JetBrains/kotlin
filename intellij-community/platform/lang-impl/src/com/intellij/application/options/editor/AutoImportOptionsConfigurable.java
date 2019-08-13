/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.application.options.editor;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.Configurable.VariableProjectAppLevel;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.project.Project;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class AutoImportOptionsConfigurable
  extends CompositeConfigurable<AutoImportOptionsProvider>
  implements EditorOptionsProvider, VariableProjectAppLevel {

  private final Project myProject;
  private JPanel myPanel;
  private JPanel myProvidersPanel;

  public AutoImportOptionsConfigurable(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  protected List<AutoImportOptionsProvider> createConfigurables() {
    return ContainerUtil.mapNotNull(AutoImportOptionsProviderEP.EP_NAME.getExtensions(myProject),
                                    (NullableFunction<ConfigurableEP<AutoImportOptionsProvider>, AutoImportOptionsProvider>)ep -> ep.createConfigurable());
  }

  @Override
  @Nls
  public String getDisplayName() {
    return ApplicationBundle.message("auto.import");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.autoimport";
  }

  @Override
  public JComponent createComponent() {
    myProvidersPanel.removeAll();
    for (int i = 0; i < getConfigurables().size(); i++) {
      AutoImportOptionsProvider provider = getConfigurables().get(i);
      myProvidersPanel.add(provider.createComponent(), new GridBagConstraints(0, i, 1, 1, 0, 0,
                                                                     GridBagConstraints.NORTH,
                                                                     GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0,0));
    }
    myProvidersPanel.add(Box.createVerticalGlue(), new GridBagConstraints(0, getConfigurables().size(), 1, 1, 0, 1,
                                                                     GridBagConstraints.NORTH,
                                                                     GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0));
    myProvidersPanel.add(Box.createVerticalGlue(), new GridBagConstraints(1, 0, getConfigurables().size() + 1, 1, 1, 0,
                                                                     GridBagConstraints.NORTH,
                                                                     GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,0));
    return myPanel;
  }

  @Override
  @NotNull
  public String getId() {
    return "editor.preferences.import";
  }

  @Override
  public boolean isProjectLevel() {
    return false;
  }
}
