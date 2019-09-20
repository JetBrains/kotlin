// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.XmlBasedSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class XmlBasedSettingsImpl implements XmlBasedSettings {
  protected final SettingsXmlFile mySettingsFile;
  protected final ConversionContextImpl myContext;

  public XmlBasedSettingsImpl(@NotNull Path file, @NotNull ConversionContextImpl context) throws CannotConvertException {
    myContext = context;
    mySettingsFile = context.getOrCreateFile(file);
  }

  @Override
  @NotNull
  public Element getRootElement() {
    return mySettingsFile.getRootElement();
  }

  @Override
  public Path getPath() {
    return mySettingsFile.getFile();
  }
}
