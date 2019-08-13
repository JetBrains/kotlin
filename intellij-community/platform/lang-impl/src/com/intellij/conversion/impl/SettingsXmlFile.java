/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import java.io.File;
import java.io.IOException;

/**
 * @author nik
 */
class SettingsXmlFile {
  private final File myFile;
  private final Document myDocument;
  private final Element myRootElement;

  SettingsXmlFile(@NotNull File file) throws CannotConvertException {
    myFile = file;
    myDocument = JDomConvertingUtil.loadDocument(file);
    myRootElement = myDocument.getRootElement();
  }

  public File getFile() {
    return myFile;
  }

  public Element getRootElement() {
    return myRootElement;
  }

  public void save() throws IOException {
    JDOMUtil.writeDocument(myDocument, myFile, SystemProperties.getLineSeparator());
  }

  @Nullable 
  public Element findComponent(String componentName) {
    return JDomSerializationUtil.findComponent(myRootElement, componentName);
  }
}
