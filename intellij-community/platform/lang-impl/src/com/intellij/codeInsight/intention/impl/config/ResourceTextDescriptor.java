// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ResourceUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author yole
 */
class ResourceTextDescriptor implements TextDescriptor {
  private final ClassLoader myLoader;
  private final String myResourcePath;

  ResourceTextDescriptor(ClassLoader loader, @NotNull String resourcePath) {
    myLoader = loader;
    myResourcePath = resourcePath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResourceTextDescriptor resource = (ResourceTextDescriptor)o;
    return Objects.equals(myLoader, resource.myLoader) &&
           Objects.equals(myResourcePath, resource.myResourcePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myLoader, myResourcePath);
  }

  @NotNull
  @Override
  public String getText() throws IOException {
    InputStream stream = myLoader.getResourceAsStream(myResourcePath);
    if (stream == null) {
      throw new IOException("Resource not found: " + myResourcePath);
    }
    return ResourceUtil.loadText(stream);
  }

  @NotNull
  @Override
  public String getFileName() {
    return StringUtil.trimEnd(myResourcePath.substring(myResourcePath.lastIndexOf('/') + 1), BeforeAfterActionMetaData.EXAMPLE_USAGE_URL_SUFFIX);
  }
}