// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model;

import java.io.File;
import java.io.Serializable;

public interface SourceFile extends Serializable {
  File getSourceFile();

  File getObjectFile();
}
