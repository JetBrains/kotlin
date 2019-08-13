/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ide.fileTemplates.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.Reference;
import java.net.URL;

/**
 * @author Eugene Zhuravlev
 */
public class DefaultTemplate {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.fileTemplates.impl.DefaultTemplate");
  
  private final String myName;
  private final String myExtension;

  private final URL myTextURL;
  private Reference<String> myText;
  
  @Nullable
  private final URL myDescriptionURL;
  private Reference<String> myDescriptionText;

  public DefaultTemplate(@NotNull String name, @NotNull String extension, @NotNull URL templateURL, @Nullable URL descriptionURL) {
    myName = name;
    myExtension = extension;
    myTextURL = templateURL;
    myDescriptionURL = descriptionURL;
  }

  @NotNull
  private static String loadText(@NotNull URL url) {
    String text = "";
    try {
      text = StringUtil.convertLineSeparators(UrlUtil.loadText(url));
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return text;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public String getQualifiedName() {
    return FileTemplateBase.getQualifiedName(getName(), getExtension());
  }

  @NotNull
  public String getExtension() {
    return myExtension;
  }

  @NotNull
  public URL getTemplateURL() {
    return myTextURL;
  }

  @NotNull
  public String getText() {
    String text = SoftReference.dereference(myText);
    if (text == null) {
      text = loadText(myTextURL);
      myText = new java.lang.ref.SoftReference<>(text);
    }
    return text;
  }

  @NotNull
  public String getDescriptionText() {
    if (myDescriptionURL == null) return "";
    String text = SoftReference.dereference(myDescriptionText);
    if (text == null) {
      text = loadText(myDescriptionURL);
      myDescriptionText = new java.lang.ref.SoftReference<>(text);
    }
    return text;
  }
}
