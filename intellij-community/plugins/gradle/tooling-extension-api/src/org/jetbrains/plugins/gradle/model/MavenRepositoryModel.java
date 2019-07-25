// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import java.io.Serializable;

/**
 * Maven repositories configured for the project
 */
public interface MavenRepositoryModel extends Serializable {
  String getName();

  String getUrl();
}
