/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.codeInspection.offlineViewer;

import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ex.*;
import com.intellij.codeInspection.offline.OfflineProblemDescriptor;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.ui.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.containers.FactoryMap;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class OfflineInspectionRVContentProvider extends InspectionRVContentProvider {
  private final Map<String, Map<String, Set<OfflineProblemDescriptor>>> myContent;
  private final Map<String, Map<OfflineProblemDescriptor, OfflineDescriptorResolveResult>> myResolvedDescriptor =
    FactoryMap.create(key -> new THashMap<>());

  public OfflineInspectionRVContentProvider(@NotNull Map<String, Map<String, Set<OfflineProblemDescriptor>>> content) {
    myContent = content;
  }

  @Override
  public boolean checkReportedProblems(@NotNull GlobalInspectionContextImpl context,
                                       @NotNull final InspectionToolWrapper toolWrapper) {
    final Map<String, Set<OfflineProblemDescriptor>> content = getFilteredContent(context, toolWrapper);
    return content != null && !content.values().isEmpty();
  }

  @Override
  public Iterable<? extends ScopeToolState> getTools(Tools tools) {
    return Collections.singletonList(tools.getDefaultState());
  }

  @NotNull
  @Override
  public QuickFixAction[] getCommonQuickFixes(@NotNull final InspectionToolWrapper toolWrapper, @NotNull final InspectionTree tree) {
    GlobalInspectionContextImpl context = tree.getContext();
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    return getCommonFixes(presentation, tree.getSelectedDescriptors());
  }

  @Override
  public boolean isContentLoaded() {
    return false;
  }

  @Override
  public void appendToolNodeContent(@NotNull GlobalInspectionContextImpl context,
                                    @NotNull InspectionToolWrapper wrapper,
                                    @NotNull InspectionTreeNode parentNode,
                                    boolean showStructure,
                                    boolean groupBySeverity, @NotNull final Map<String, Set<RefEntity>> contents,
                                    @NotNull Function<? super RefEntity, CommonProblemDescriptor[]> problems) {
    final Map<String, Set<OfflineProblemDescriptor>> filteredContent = getFilteredContent(context, wrapper);
    InspectionResultsView view = context.getView();
    if (filteredContent != null && !filteredContent.values().isEmpty()) {
      buildTree(context, filteredContent, wrapper, descriptor -> {
                  final RefEntity element = descriptor.getRefElement(context.getRefManager());
                  return new RefEntityContainer<OfflineProblemDescriptor>(element, new OfflineProblemDescriptor[] {descriptor}) {
                    @Nullable
                    @Override
                    protected String getModuleName() {
                      final String module = super.getModuleName();
                      return module == null ? descriptor.getModuleName() : module;
                    }
                  };
                }, showStructure,
                parentNode, view.getTree().getInspectionTreeModel());
    }
  }

  @Nullable
  @SuppressWarnings({"UnusedAssignment"})
  private Map<String, Set<OfflineProblemDescriptor>> getFilteredContent(@NotNull GlobalInspectionContextImpl context,
                                                                        @NotNull InspectionToolWrapper toolWrapper) {
    Map<String, Set<OfflineProblemDescriptor>> content = myContent.get(toolWrapper.getShortName());
    if (content == null) return null;
    if (context.getUIOptions().FILTER_RESOLVED_ITEMS) {
      final Map<String, Set<OfflineProblemDescriptor>> current = new HashMap<>(content);
      content = null; //GC it
      Map<OfflineProblemDescriptor, OfflineDescriptorResolveResult> resolvedDescriptors = myResolvedDescriptor.get(toolWrapper.getShortName());
      resolvedDescriptors.forEach((descriptor, descriptorResolveResult) -> {
        if (descriptorResolveResult.isExcluded()) {
          RefEntity entity = descriptorResolveResult.getResolvedEntity();
          if (entity != null) {
            excludeProblem(entity.getExternalName(), current);
          }
        }
      });
      InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
      for (RefEntity refEntity : presentation.getResolvedElements()) {
        //TODO
        if (refEntity instanceof RefElement) {
          excludeProblem(refEntity.getExternalName(), current);
        }
      }
      return current;
    }
    return content;
  }

  private static void excludeProblem(final String externalName, final Map<String, Set<OfflineProblemDescriptor>> content) {
    for (Iterator<String> iter = content.keySet().iterator(); iter.hasNext();) {
      final String packageName = iter.next();
      final Set<OfflineProblemDescriptor> excluded = new HashSet<>(content.get(packageName));
      for (Iterator<OfflineProblemDescriptor> it = excluded.iterator(); it.hasNext();) {
        final OfflineProblemDescriptor ex = it.next();
        if (Comparing.strEqual(ex.getFQName(), externalName)) {
          it.remove();
        }
      }
      if (excluded.isEmpty()) {
        iter.remove();
      } else {
        content.put(packageName, excluded);
      }
    }
  }

  @Override
  protected void appendDescriptor(@NotNull GlobalInspectionContextImpl context,
                                  @NotNull final InspectionToolWrapper toolWrapper,
                                  @NotNull final RefEntityContainer container,
                                  @NotNull final InspectionTreeNode parent) {
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    InspectionTreeModel model = context.getView().getTree().getInspectionTreeModel();
    for (OfflineProblemDescriptor descriptor : ((RefEntityContainer<OfflineProblemDescriptor>)container).getDescriptors()) {
      final OfflineDescriptorResolveResult resolveResult = myResolvedDescriptor.get(toolWrapper.getShortName())
        .computeIfAbsent(descriptor, d -> OfflineDescriptorResolveResult.resolve(d, toolWrapper, presentation));
      RefEntity resolvedEntity = resolveResult.getResolvedEntity();
      CommonProblemDescriptor resolvedDescriptor = resolveResult.getResolvedDescriptor();
      if (resolvedEntity != null && resolvedDescriptor != null) {
        presentation.getProblemElements().put(resolvedEntity, resolvedDescriptor);
        model.createProblemDescriptorNode(resolvedEntity, resolvedDescriptor, presentation, parent);
      } else {
        model.createOfflineProblemDescriptorNode(descriptor, resolveResult, presentation, parent);
      }
    }
  }
}
