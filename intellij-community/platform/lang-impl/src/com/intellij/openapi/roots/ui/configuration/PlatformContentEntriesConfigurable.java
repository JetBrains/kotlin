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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.ModuleConfigurationStateImpl;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author yole
 */
public class PlatformContentEntriesConfigurable implements Configurable {
  private final Module myModule;
  private final JpsModuleSourceRootType<?>[] myRootTypes;
  private final JPanel myTopPanel = new JPanel(new BorderLayout());
  private ModifiableRootModel myModifiableModel;
  private CommonContentEntriesEditor myEditor;

  public PlatformContentEntriesConfigurable(final Module module, JpsModuleSourceRootType<?>... rootTypes) {
    myModule = module;
    myRootTypes = rootTypes;
  }

  @Override
  public String getDisplayName() {
    return "Project Structure";
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    createEditor();
    return myTopPanel;
  }

  private void createEditor() {
    myModifiableModel =
      ReadAction.compute(() -> ModuleRootManager.getInstance(myModule).getModifiableModel());

    final ModuleConfigurationStateImpl moduleConfigurationState =
      new ModuleConfigurationStateImpl(myModule.getProject(), new DefaultModulesProvider(myModule.getProject())) {
        @Override
        public ModifiableRootModel getRootModel() {
          return myModifiableModel;
        }
      };
    myEditor = new CommonContentEntriesEditor(myModule.getName(), moduleConfigurationState, true, myRootTypes) {
      @Override
      protected List<ContentEntry> addContentEntries(VirtualFile[] files) {
        List<ContentEntry> entries = super.addContentEntries(files);
        addContentEntryPanels(entries.toArray(new ContentEntry[0]));
        return entries;
      }
    };
    JComponent component = ReadAction.compute(() -> myEditor.createComponent());
    myTopPanel.add(component, BorderLayout.CENTER);
  }

  @Override
  public boolean isModified() {
    return myEditor != null && myEditor.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myEditor.apply();
    if (myModifiableModel.isChanged()) {
      ApplicationManager.getApplication().runWriteAction(() -> myModifiableModel.commit());
      myEditor.disposeUIResources();
      myTopPanel.remove(myEditor.getComponent());
      createEditor();
    }
  }

  @Override
  public void reset() {
    myEditor.reset();
    // TODO?
  }

  @Override
  public void disposeUIResources() {
    if (myEditor != null) {
      myEditor.disposeUIResources();
      myTopPanel.remove(myEditor.getComponent());
      myEditor = null;
    }
    if (myModifiableModel != null) {
      myModifiableModel.dispose();
      myModifiableModel = null;
    }
  }
}
