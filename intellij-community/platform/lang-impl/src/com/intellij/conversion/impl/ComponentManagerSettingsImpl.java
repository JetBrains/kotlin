// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ComponentManagerSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

/**
 * @author nik
 */
public class ComponentManagerSettingsImpl extends XmlBasedSettingsImpl implements ComponentManagerSettings {
  protected ComponentManagerSettingsImpl(@NotNull Path file, @NotNull ConversionContextImpl context) throws CannotConvertException {
    super(file, context);
  }

  /**
   * @deprecated Use {@link #ComponentManagerSettingsImpl(Path, ConversionContextImpl)}
   */
  @Deprecated
  protected ComponentManagerSettingsImpl(File file, ConversionContextImpl context) throws CannotConvertException {
    super(file.toPath(), context);
  }

  @Override
  public Element getComponentElement(@NotNull @NonNls String componentName) {
    return mySettingsFile.findComponent(componentName);
  }
}
