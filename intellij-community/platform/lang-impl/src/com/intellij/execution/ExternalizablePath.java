/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.execution;

import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public class ExternalizablePath implements JDOMExternalizable {
  @NonNls private static final String VALUE_ATTRIBUTE = "value";

  private String myUrl;

  @Override
  public void readExternal(final Element element) throws InvalidDataException {
    final String value = element.getAttributeValue(VALUE_ATTRIBUTE);
    myUrl = value != null ? value : "";
    final String protocol = VirtualFileManager.extractProtocol(myUrl);
    if (protocol == null) myUrl = urlValue(myUrl);
  }

  @Override
  public void writeExternal(final Element element) {
    element.setAttribute(VALUE_ATTRIBUTE, myUrl);
  }

  public String getLocalPath() {
    return localPathValue(myUrl);
  }

  public static String urlValue(String localPath) {
    return StringUtil.isEmptyOrSpaces(localPath) ? "" : VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, FileUtilRt.toSystemIndependentName(localPath.trim()));
  }

  public static String localPathValue(@Nullable String url) {
    return StringUtil.isEmptyOrSpaces(url) ? "" : FileUtilRt.toSystemDependentName(VirtualFileManager.extractPath(url.trim()));
  }
}
