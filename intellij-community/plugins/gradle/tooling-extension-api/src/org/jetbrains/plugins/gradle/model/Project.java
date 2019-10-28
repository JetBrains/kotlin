// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.model.ProjectModel;

/**
 * @author Vladislav.Soroka
 */
public interface Project extends ProjectModel {
  String getName();
}
