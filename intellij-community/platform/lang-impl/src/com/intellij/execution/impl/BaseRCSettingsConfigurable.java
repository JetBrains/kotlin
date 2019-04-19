// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorConfigurable;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * This class provides 'smart' isModified() behavior: it compares original settings with current snapshot by their XML 'externalized' presentations
 */
abstract class BaseRCSettingsConfigurable extends SettingsEditorConfigurable<RunnerAndConfigurationSettings> {
  BaseRCSettingsConfigurable(@NotNull SettingsEditor<RunnerAndConfigurationSettings> editor, @NotNull RunnerAndConfigurationSettings settings) {
    super(editor, settings);
  }

  @Override
  public boolean isModified() {
    try {
      RunnerAndConfigurationSettings original = getSettings();

      final RunManagerImpl runManager = ((RunnerAndConfigurationSettingsImpl)original).getManager();
      if (!original.isTemplate() && !runManager.hasSettings(original)) {
        return true;
      }
      if (!super.isModified()) {
        return false;
      }

      RunnerAndConfigurationSettings snapshot = getEditor().getSnapshot();
      if (isSnapshotSpecificallyModified(original, snapshot) || !RunManagerImplKt.doGetBeforeRunTasks(original.getConfiguration()).equals(RunManagerImplKt.doGetBeforeRunTasks(snapshot.getConfiguration()))) {
        return true;
      }

      if (original instanceof JDOMExternalizable && snapshot instanceof JDOMExternalizable) {
        applySnapshotToComparison(original, snapshot);

        Element originalElement = new Element("config");
        Element snapshotElement = new Element("config");
        ((JDOMExternalizable)original).writeExternal(originalElement);
        ((JDOMExternalizable)snapshot).writeExternal(snapshotElement);
        patchElementsIfNeed(originalElement, snapshotElement);
        boolean result = !JDOMUtil.areElementsEqual(originalElement, snapshotElement, true);
        if (!result) {
          super.setModified(false);
        }
        return result;
      }
    }
    catch (ConfigurationException e) {
      //ignore
    }
    return super.isModified();
  }

  void applySnapshotToComparison(RunnerAndConfigurationSettings original, RunnerAndConfigurationSettings snapshot) {}

  boolean isSnapshotSpecificallyModified(@NotNull RunnerAndConfigurationSettings original, @NotNull RunnerAndConfigurationSettings snapshot) {
    return false;
  }

  void patchElementsIfNeed(Element originalElement, Element snapshotElement) {}
}
