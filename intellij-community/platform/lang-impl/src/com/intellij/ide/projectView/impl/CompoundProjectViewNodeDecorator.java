// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * This class is intended to combine all decorators for batch usages.
 */
public final class CompoundProjectViewNodeDecorator implements ProjectViewNodeDecorator {
  private static final ProjectViewNodeDecorator EMPTY = new CompoundProjectViewNodeDecorator(null);
  private static final Key<ProjectViewNodeDecorator> KEY = Key.create("ProjectViewNodeDecorator");
  private static final Logger LOG = Logger.getInstance(CompoundProjectViewNodeDecorator.class);
  private final Project myProject;

  /**
   * @return a shared instance for the specified project
   */
  @NotNull
  public static ProjectViewNodeDecorator get(@Nullable Project project) {
    if (project == null || project.isDisposed() || project.isDefault()) return EMPTY;
    ProjectViewNodeDecorator provider = project.getUserData(KEY);
    if (provider != null) return provider;
    provider = new CompoundProjectViewNodeDecorator(project);
    project.putUserData(KEY, provider);
    return provider;
  }

  private CompoundProjectViewNodeDecorator(@Nullable Project project) {
    myProject = project;
  }

  @Override
  public void decorate(ProjectViewNode node, PresentationData data) {
    forEach(decorator -> decorator.decorate(node, data));
  }

  @Override
  public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {
    forEach(decorator -> decorator.decorate(node, cellRenderer));
  }

  private void forEach(@NotNull Consumer<? super ProjectViewNodeDecorator> consumer) {
    if (myProject == null || myProject.isDisposed()) return; // empty or disposed
    for (ProjectViewNodeDecorator decorator : EP.getExtensions(myProject)) {
      try {
        consumer.accept(decorator);
      }
      catch (IndexNotReadyException exception) {
        throw new ProcessCanceledException(exception);
      }
      catch (ProcessCanceledException exception) {
        throw exception;
      }
      catch (Exception exception) {
        LOG.warn("unexpected error in " + decorator, exception);
      }
    }
  }
}
