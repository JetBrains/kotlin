// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.structureView.impl;

import com.intellij.ide.impl.StructureViewWrapperImpl;
import com.intellij.ide.structureView.*;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.PsiElement;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.Collections;

@State(name = "StructureViewFactory", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class StructureViewFactoryImpl extends StructureViewFactoryEx implements PersistentStateComponent<StructureViewFactoryImpl.State> {
  public static class State {
    @SuppressWarnings({"WeakerAccess"}) public boolean AUTOSCROLL_MODE = true;
    @SuppressWarnings({"WeakerAccess"}) public boolean AUTOSCROLL_FROM_SOURCE = false;
    @SuppressWarnings({"WeakerAccess"}) public String ACTIVE_ACTIONS = "";
  }

  private final Project myProject;
  private StructureViewWrapperImpl myStructureViewWrapperImpl;
  private State myState = new State();
  private Runnable myRunWhenInitialized = null;

  private static final NotNullLazyValue<MultiValuesMap<Class<? extends PsiElement>, StructureViewExtension>> myExtensions = new NotNullLazyValue<MultiValuesMap<Class<? extends PsiElement>, StructureViewExtension>>() {
    @NotNull
    @Override
    protected MultiValuesMap<Class<? extends PsiElement>, StructureViewExtension> compute() {
      MultiValuesMap<Class<? extends PsiElement>, StructureViewExtension> map =
        new MultiValuesMap<>();
      for (StructureViewExtension extension : StructureViewExtension.EXTENSION_POINT_NAME.getExtensionList()) {
        map.put(extension.getType(), extension);
      }
      return map;
    }
  };
  private final MultiValuesMap<Class<? extends PsiElement>, StructureViewExtension> myImplExtensions = new MultiValuesMap<>();

  public StructureViewFactoryImpl(Project project) {
    myProject = project;
  }

  @Override
  public StructureViewWrapper getStructureViewWrapper() {
    return myStructureViewWrapperImpl;
  }

  @Override
  @NotNull
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state;
  }

  public void initToolWindow(@NotNull ToolWindowEx toolWindow) {
    myStructureViewWrapperImpl = new StructureViewWrapperImpl(myProject, toolWindow);
    if (myRunWhenInitialized != null) {
      myRunWhenInitialized.run();
      myRunWhenInitialized = null;
    }
  }

  @NotNull
  @Override
  public Collection<StructureViewExtension> getAllExtensions(@NotNull Class<? extends PsiElement> type) {
    Collection<StructureViewExtension> result = myImplExtensions.get(type);
    if (result == null) {
      MultiValuesMap<Class<? extends PsiElement>, StructureViewExtension> map = myExtensions.getValue();
      for (Class<? extends PsiElement> registeredType : map.keySet()) {
        if (ReflectionUtil.isAssignable(registeredType, type)) {
          final Collection<StructureViewExtension> extensions = map.get(registeredType);
          for (StructureViewExtension extension : extensions) {
            myImplExtensions.put(type, extension);
          }
        }
      }
      result = myImplExtensions.get(type);
      if (result == null) return Collections.emptyList();
    }
    return result;
  }

  @Override
  public void setActiveAction(final String name, final boolean state) {
    Collection<String> activeActions = collectActiveActions();

    if (state) {
      activeActions.add(name);
    }
    else {
      activeActions.remove(name);
    }

    myState.ACTIVE_ACTIONS = toString(activeActions);
  }

  private static String toString(final Collection<String> activeActions) {
    return StringUtil.join(activeActions, ",");
  }

  public Collection<String> collectActiveActions() {
    return ContainerUtil.newLinkedHashSet(myState.ACTIVE_ACTIONS.split(","));
  }

  @Override
  public boolean isActionActive(final String name) {
    return collectActiveActions().contains(name);
  }

  @Override
  public void runWhenInitialized(@NotNull Runnable runnable) {
    if (myStructureViewWrapperImpl != null) {
      runnable.run();
    }
    else {
      myRunWhenInitialized = runnable;
    }
  }

  @NotNull
  @Override
  public StructureView createStructureView(final FileEditor fileEditor,
                                           @NotNull final StructureViewModel treeModel,
                                           @NotNull final Project project) {
    return createStructureView(fileEditor, treeModel, project, true);
  }

  @NotNull
  @Override
  public StructureView createStructureView(final FileEditor fileEditor,
                                           @NotNull StructureViewModel treeModel,
                                           @NotNull Project project,
                                           final boolean showRootNode) {
    return new StructureViewComponent(fileEditor, treeModel, project, showRootNode);
  }

  @TestOnly
  public void cleanupForNextTest() {
    assert ApplicationManager.getApplication().isUnitTestMode();
    myState = new State();
  }
}
