// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.options;

import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.Validator;
import com.intellij.openapi.compiler.options.ExcludedEntriesConfiguration;
import com.intellij.openapi.compiler.options.ExcludesConfiguration;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.java.compiler.JpsCompilerValidationExcludeSerializer;
import org.jetbrains.jps.model.serialization.java.compiler.JpsValidationSerializer;

/**
 * @author Dmitry Avdeev
 */
@State(name = JpsValidationSerializer.COMPONENT_NAME, storages = @Storage(JpsValidationSerializer.CONFIG_FILE_NAME))
public class ValidationConfiguration implements PersistentStateComponent<JpsValidationSerializer.ValidationConfigurationState> {
  private final JpsValidationSerializer.ValidationConfigurationState myState = new JpsValidationSerializer.ValidationConfigurationState();
  private final Project myProject;

  public ValidationConfiguration(Project project) {
    myProject = project;
  }

  public static boolean shouldValidate(Compiler validator, Project project) {
    ValidationConfiguration configuration = getInstance(project);
    return (configuration.myState.VALIDATE_ON_BUILD) && configuration.isSelected(validator);
  }

  public boolean isSelected(Compiler validator) {
    return isSelected(validator.getDescription());
  }

  public boolean isSelected(String validatorDescription) {
    final Boolean selected = myState.VALIDATORS.get(validatorDescription);
    return selected == null || selected.booleanValue();
  }

  public boolean isValidateOnBuild() {
    return myState.VALIDATE_ON_BUILD;
  }

  public void setValidateOnBuild(boolean value) {
    myState.VALIDATE_ON_BUILD = value;
  }

  public void setSelected(Compiler validator, boolean selected) {
    setSelected(validator.getDescription(), selected);
  }

  public void deselectAllValidators() {
    for (Validator validator : CompilerManager.getInstance(myProject).getCompilers(Validator.class)) {
      myState.VALIDATORS.put(validator.getDescription(), false);
    }
  }

  public void setSelected(String validatorDescription, boolean selected) {
    myState.VALIDATORS.put(validatorDescription, selected);
  }

  public static ValidationConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, ValidationConfiguration.class);
  }

  public static ExcludesConfiguration getExcludedEntriesConfiguration(Project project) {
    return ServiceManager.getService(project, ExcludedFromValidationConfiguration.class);
  }

  @Override
  @NotNull
  public JpsValidationSerializer.ValidationConfigurationState getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull final JpsValidationSerializer.ValidationConfigurationState state) {
    XmlSerializerUtil.copyBean(state, myState);
  }

  @State(
    name = JpsCompilerValidationExcludeSerializer.COMPONENT_NAME,
    storages = @Storage(JpsCompilerValidationExcludeSerializer.CONFIG_FILE_NAME)
  )
  public static class ExcludedFromValidationConfiguration extends ExcludedEntriesConfiguration {
    public ExcludedFromValidationConfiguration() {
      super(null);
    }
  }
}
