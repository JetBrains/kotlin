// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.conversion;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

public interface XmlBasedSettings {
  @NotNull
  Element getRootElement();

  /**
   * @deprecated Use {@link #getPath()}
   */
  @Deprecated
  default File getFile() {
    throw new AbstractMethodError();
  }

  default Path getPath() {
    return getFile().toPath();
  }
}
