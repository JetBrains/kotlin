// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.configuration;

import com.intellij.execution.configurations.RunConfiguration;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for run configurations that need to customize serialization/deserialization of persistent state.
 *
 * @see com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
 */
public interface PersistentAwareRunConfiguration extends RunConfiguration {
  /**
   * Sets whether the run configuration is template
   */
  void setTemplate(boolean isTemplate);

  /**
   * This method is called to read persistent state while {@link RunConfiguration#readExternal} is called to read state in other cases.
   */
  void readPersistent(@NotNull Element element);
  /**
   * This method is called to write persistent state while {@link RunConfiguration#writeExternal} is called to write state in other cases.
   */
  void writePersistent(@NotNull Element element);

  /**
   * Returns whether the run configuration contains any data that must not be persisted within it anymore (eg. passwords).
   */
  boolean needsToBeMigrated();
}
