// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.structureView.newStructureView;

import com.intellij.ide.structureView.FileEditorPositionListener;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.*;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TreeModelWrapper implements StructureViewModel, ProvidingTreeModel {
  private final StructureViewModel myModel;
  private final TreeActionsOwner myStructureView;

  public TreeModelWrapper(@NotNull StructureViewModel model, @NotNull TreeActionsOwner structureView) {
    myModel = model;
    myStructureView = structureView;
  }

  @Override
  @NotNull
  public StructureViewTreeElement getRoot() {
    return myModel.getRoot();
  }

  @Override
  @NotNull
  public Grouper[] getGroupers() {
    List<Grouper> filtered = filterActive(myModel.getGroupers());
    return filtered.toArray(Grouper.EMPTY_ARRAY);
  }

  @NotNull
  private <T extends TreeAction> List<T> filterActive(@NotNull T[] actions) {
    List<T> filtered = new ArrayList<>();
    for (T action : actions) {
      if (isFiltered(action)) filtered.add(action);
    }
    return filtered;
  }

  @NotNull
  private List<NodeProvider> filterProviders(@NotNull Collection<NodeProvider> actions) {
    List<NodeProvider> filtered = new ArrayList<>();
    for (NodeProvider action : actions) {
      if (isFiltered(action)) filtered.add(action);
    }
    return filtered;
  }

  private boolean isFiltered(@NotNull TreeAction action) {
    return action instanceof Sorter && !((Sorter)action).isVisible() || myStructureView.isActionActive(action.getName());
  }

  @Override
  @NotNull
  public Sorter[] getSorters() {
    List<Sorter> filtered = filterActive(myModel.getSorters());
    return filtered.toArray(Sorter.EMPTY_ARRAY);
  }

  @Override
  @NotNull
  public Filter[] getFilters() {
    List<Filter> filtered = filterActive(myModel.getFilters());
    return filtered.toArray(Filter.EMPTY_ARRAY);
  }

  @Override
  public Object getCurrentEditorElement() {
    return myModel.getCurrentEditorElement();
  }

  @NotNull
  @Override
  public Collection<NodeProvider> getNodeProviders() {
    if (myModel instanceof ProvidingTreeModel) {
      return filterProviders(((ProvidingTreeModel)myModel).getNodeProviders());
    }
    return Collections.emptyList();
  }

  public static boolean isActive(@NotNull TreeAction action, @NotNull TreeActionsOwner actionsOwner) {
    if (shouldRevert(action)) {
      return !actionsOwner.isActionActive(action.getName());
    }
    return action instanceof Sorter && !((Sorter)action).isVisible() || actionsOwner.isActionActive(action.getName());
  }

  public static boolean shouldRevert(@NotNull TreeAction action) {
    return action instanceof Filter && ((Filter)action).isReverted();
  }

  @Override
  public void addEditorPositionListener(@NotNull FileEditorPositionListener listener) {
    myModel.addEditorPositionListener(listener);
  }

  @Override
  public void removeEditorPositionListener(@NotNull FileEditorPositionListener listener) {
    myModel.removeEditorPositionListener(listener);
  }

  @Override
  public void dispose() {
    Disposer.dispose(myModel);
  }

  @Override
  public boolean shouldEnterElement(final Object element) {
    return false;
  }

  @Override
  public void addModelListener(@NotNull ModelListener modelListener) {
    myModel.addModelListener(modelListener);
  }

  @Override
  public void removeModelListener(@NotNull ModelListener modelListener) {
    myModel.removeModelListener(modelListener);
  }

  public StructureViewModel getModel() {
    return myModel;
  }

  @Override
  public boolean isEnabled(@NotNull NodeProvider provider) {
    return myStructureView.isActionActive(provider.getName());
  }
}
