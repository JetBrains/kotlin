// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import org.jetbrains.plugins.gradle.model.ExternalProject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Vladislav.Soroka
 */
public class ProjectHierarchyIterator implements Iterator<ExternalProject> {
  private final Queue<ExternalProject> myProjects = new LinkedList<ExternalProject>();

  public ProjectHierarchyIterator(ExternalProject project) {
    myProjects.add(project);
  }

  @Override
  public boolean hasNext() {
    return !myProjects.isEmpty();
  }

  @Override
  public ExternalProject next() {
    ExternalProject project = myProjects.remove();
    myProjects.addAll(project.getChildProjects().values());
    return project;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove");
  }
}
