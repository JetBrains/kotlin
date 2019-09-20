// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.ide.impl.convert.JDomConvertingUtil;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.SystemProperties;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.JDomSerializationUtil;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author nik
 */
class SettingsXmlFile {
  private final Path myFile;
  private final Document myDocument;
  private final Element myRootElement;

  SettingsXmlFile(@NotNull Path file) throws CannotConvertException {
    myFile = file;
    myDocument = JDomConvertingUtil.loadDocument(file.toFile());
    myRootElement = myDocument.getRootElement();
  }

  public Path getFile() {
    return myFile;
  }

  public Element getRootElement() {
    return myRootElement;
  }

  public void save() throws IOException {
    JDOMUtil.writeDocument(myDocument, myFile.toFile(), SystemProperties.getLineSeparator());
  }

  @Nullable
  public Element findComponent(String componentName) {
    return JDomSerializationUtil.findComponent(myRootElement, componentName);
  }
}
