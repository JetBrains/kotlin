// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;

import java.io.Serializable;

/**
 * @author Vladislav.Soroka
 */
public interface ExtIdeaContentRoot extends IdeaContentRoot, Serializable {

  @Override
  DomainObjectSet<? extends IdeaSourceDirectory> getResourceDirectories();

  @Override
  DomainObjectSet<? extends IdeaSourceDirectory> getTestResourceDirectories();
}
