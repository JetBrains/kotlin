/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.packaging.impl.elements;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.ui.ArtifactEditorContext;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class FacetBasedPackagingElementType<E extends PackagingElement<?>, F extends Facet> extends PackagingElementType<E> {
  private final FacetTypeId<F> myFacetType;

  protected FacetBasedPackagingElementType(@NotNull @NonNls String id, @NotNull String presentableName, FacetTypeId<F> facetType) {
    super(id, presentableName);
    myFacetType = facetType;
  }

  @Override
  public boolean canCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
    return !getFacets(context).isEmpty();
  }

  @Override
  public Icon getCreateElementIcon() {
    return FacetTypeRegistry.getInstance().findFacetType(myFacetType).getIcon();
  }

  @NotNull
  @Override
  public List<? extends E> chooseAndCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact, @NotNull CompositePackagingElement<?> parent) {
    final List<F> facets = getFacets(context);
    ChooseFacetsDialog dialog = new ChooseFacetsDialog(context.getProject(), facets, getDialogTitle(), getDialogDescription());
    if (dialog.showAndGet()) {
      final List<E> elements = new ArrayList<>();
      for (F facet : dialog.getChosenElements()) {
        elements.add(createElement(context.getProject(), facet));
      }
      return elements;
    }
    return Collections.emptyList();
  }

  private List<F> getFacets(ArtifactEditorContext context) {
    final Module[] modules = context.getModulesProvider().getModules();
    final List<F> facets = new ArrayList<>();
    for (Module module : modules) {
      facets.addAll(context.getFacetsProvider().getFacetsByType(module, myFacetType));
    }
    return facets;
  }

  protected abstract E createElement(Project project, F facet);

  protected abstract String getDialogTitle();

  protected abstract String getDialogDescription();

  protected abstract String getItemText(F item);

  private class ChooseFacetsDialog extends ChooseElementsDialog<F> {
    private ChooseFacetsDialog(Project project, List<? extends F> items, String title, String description) {
      super(project, items, title, description, true);
    }

    @Override
    protected String getItemText(F item) {
      return FacetBasedPackagingElementType.this.getItemText(item);
    }

    @Override
    protected Icon getItemIcon(F item) {
      return FacetTypeRegistry.getInstance().findFacetType(myFacetType).getIcon();
    }
  }
}
