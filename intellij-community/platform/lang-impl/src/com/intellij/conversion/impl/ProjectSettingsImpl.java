// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ProjectSettings;

import java.nio.file.Path;

/**
 * @author nik
 */
public class ProjectSettingsImpl extends ComponentManagerSettingsImpl implements ProjectSettings {
  public ProjectSettingsImpl(Path file, ConversionContextImpl context) throws CannotConvertException {
    super(file, context);
  }
}
