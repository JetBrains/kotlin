// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl.compiler;

import org.jetbrains.jps.model.java.compiler.JpsValidationConfiguration;

import java.util.Set;

public class JpsValidationConfigurationImpl implements JpsValidationConfiguration {
  private final boolean myValidateOnBuild;
  private final Set<String> myDisabledValidators;

  public JpsValidationConfigurationImpl(boolean validateOnBuild, Set<String> disabledValidators) {
    myValidateOnBuild = validateOnBuild;
    myDisabledValidators = disabledValidators;
  }

  @Override
  public boolean isValidateOnBuild() {
    return myValidateOnBuild;
  }

  @Override
  public boolean isValidatorEnabled(String validatorId) {
    return !myDisabledValidators.contains(validatorId);
  }
}
