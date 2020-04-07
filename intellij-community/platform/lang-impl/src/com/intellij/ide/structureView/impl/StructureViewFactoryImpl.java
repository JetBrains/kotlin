// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.structureView.impl;

import com.intellij.ide.impl.StructureViewWrapperImpl;
import com.intellij.ide.structureView.*;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiElement;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

@State(name = "StructureViewFactory", storages = {
  @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
})
public final class StructureViewFactoryImpl extends StructureViewFactoryEx implements PersistentStateComponent<StructureViewFactoryImpl.State> {
  public static final class State {
    @SuppressWarnings({"WeakerAccess"}) public boolean AUTOSCROLL_MODE = true;
    @SuppressWarnings({"WeakerAccess"}) public boolean AUTOSCROLL_FROM_SOURCE = false;
    @SuppressWarnings({"WeakerAccess"}) public String ACTIVE_ACTIONS = "";
  }

  private final Project myProject;
  private StructureViewWrapperImpl myStructureViewWrapperImpl;
  private State myState = new State();
  private Runnable myRunWhenInitialized = null;

  private final MultiMap<Class<? extends PsiElement>, StructureViewExtension> myImplExtensions = MultiMap.createConcurrentSet();

  public StructureViewFactoryImpl(@NotNull Project project) {
    myProject = project;
    StructureViewExtension.EXTENSION_POINT_NAME.addExtensionPointListener(() -> {
      myImplExtensions.clear();
      if (myStructureViewWrapperImpl != null) {
        myStructureViewWrapperImpl.rebuild();
      }
    }, project);
  }

  @Override
  public StructureViewWrapper getStructureViewWrapper() {
    return myStructureViewWrapperImpl;
  }

  @Override
  public @NotNull State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state;
  }

  public void initToolWindow(@NotNull ToolWindow toolWindow) {
    myStructureViewWrapperImpl = new StructureViewWrapperImpl(myProject, toolWindow);
    if (myRunWhenInitialized != null) {
      myRunWhenInitialized.run();
      myRunWhenInitialized = null;
    }
  }

  @Override
  public @NotNull Collection<StructureViewExtension> getAllExtensions(@NotNull Class<? extends PsiElement> type) {
    Collection<StructureViewExtension> result = myImplExtensions.get(type);
    if (!result.isEmpty()) {
      return result;
    }

    ExtensionManager extensionManager = ApplicationManager.getApplication().getService(ExtensionManager.class);
    assert extensionManager != null;
    Map<Class<? extends PsiElement>, List<StructureViewExtension>> map = extensionManager.extensions.getValue();
    for (Class<? extends PsiElement> registeredType : map.keySet()) {
      if (ReflectionUtil.isAssignable(registeredType, type)) {
        for (StructureViewExtension extension : map.get(registeredType)) {
          myImplExtensions.putValue(type, extension);
        }
      }
    }
    result = myImplExtensions.get(type);
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

  public @NotNull Collection<String> collectActiveActions() {
    return new LinkedHashSet<>(Arrays.asList(myState.ACTIVE_ACTIONS.split(",")));
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

  @Override
  public @NotNull StructureView createStructureView(FileEditor fileEditor, @NotNull StructureViewModel treeModel, @NotNull Project project) {
    return createStructureView(fileEditor, treeModel, project, true);
  }

  @Override
  public @NotNull StructureView createStructureView(FileEditor fileEditor,
                                                    @NotNull StructureViewModel treeModel,
                                                    @NotNull Project project,
                                                    boolean showRootNode) {
    return new StructureViewComponent(fileEditor, treeModel, project, showRootNode);
  }

  @TestOnly
  public void cleanupForNextTest() {
    assert ApplicationManager.getApplication().isUnitTestMode();
    myState = new State();
  }
}

@Service
final class ExtensionManager {
  final ClearableLazyValue<Map<Class<? extends PsiElement>, List<StructureViewExtension>>> extensions = ClearableLazyValue.create(() -> {
    Map<Class<? extends PsiElement>, List<StructureViewExtension>> map = new THashMap<>();
    for (StructureViewExtension extension : StructureViewExtension.EXTENSION_POINT_NAME.getExtensionList()) {
      List<StructureViewExtension> list = map.get(extension.getType());
      if (list == null) {
        list = new SmartList<>();
        map.put(extension.getType(), list);
      }
      list.add(extension);
    }
    return map;
  });

  ExtensionManager() {
    StructureViewExtension.EXTENSION_POINT_NAME.addExtensionPointListener(extensions::drop, null);
  }
}