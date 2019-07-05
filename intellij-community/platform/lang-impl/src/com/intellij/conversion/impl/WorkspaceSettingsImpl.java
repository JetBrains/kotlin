// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.WorkspaceSettings;

import java.nio.file.Path;

/**
 * @author nik
 */
public class WorkspaceSettingsImpl extends ComponentManagerSettingsImpl implements WorkspaceSettings {
  public WorkspaceSettingsImpl(Path workspaceFile, ConversionContextImpl context) throws CannotConvertException {
    super(workspaceFile, context);
  }
}
