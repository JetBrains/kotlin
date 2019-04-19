// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.reference.RefDirectory;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefModule;
import com.intellij.codeInspection.ui.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.TreeTraversal;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;

public abstract class InspectionRVContentProvider {
  private static final Logger LOG = Logger.getInstance(InspectionRVContentProvider.class);

  public InspectionRVContentProvider() {
  }

  protected static class RefEntityContainer<Descriptor> {
    private final Descriptor[] myDescriptors;
    @Nullable
    private final RefEntity myEntity;

    public RefEntityContainer(@Nullable RefEntity entity, Descriptor[] descriptors) {
      myEntity = entity;
      myDescriptors = descriptors;
    }

    @NotNull
    public RefElementNode createNode(@NotNull InspectionToolPresentation presentation,
                                     InspectionTreeModel model,
                                     InspectionTreeNode topParent,
                                     boolean showStructure) {
      RefEntityContainer<Descriptor> owner = getOwner();
      InspectionTreeNode parent;
      if (owner == null) {
        parent = topParent;
      } else {
        parent = owner.createNode(presentation, model, topParent, showStructure);
        if (!showStructure) {
          return (RefElementNode)parent;
        }
      }
      return model.createRefElementNode(myEntity, () -> presentation.createRefNode(myEntity, model, parent), parent);
    }

    @Nullable
    public RefEntity getRefEntity() {
      return myEntity;
    }

    @Nullable
    protected String getModuleName() {
      final RefModule refModule = myEntity instanceof RefElement
                                  ? ((RefElement)myEntity).getModule()
                                  : myEntity instanceof RefModule ? (RefModule)myEntity : null;
      return refModule != null ? refModule.getName() : null;
    }

    @Nullable
    public Module getModule(Project project) {
      String name = getModuleName();
      if (name == null) return null;
      return ReadAction.compute(() -> ModuleManager.getInstance(project).findModuleByName(name));
    }

    boolean supportStructure() {
      return myEntity == null || myEntity instanceof RefElement && !(myEntity instanceof RefDirectory); //do not show structure for refModule and refPackage
    }

    public Descriptor[] getDescriptors() {
      return myDescriptors;
    }

    @Nullable
    private RefEntityContainer<Descriptor> getOwner() {
      if (myEntity == null) return null;
      final RefEntity entity = myEntity.getOwner();
      return entity instanceof RefElement && !(entity instanceof RefDirectory)
             ? new RefEntityContainer<>(entity, myDescriptors)
             : null;
    }
  }

  public abstract boolean checkReportedProblems(@NotNull GlobalInspectionContextImpl context, @NotNull InspectionToolWrapper toolWrapper);

  public Iterable<? extends ScopeToolState> getTools(Tools tools) {
    return tools.getTools();
  }

  public boolean hasQuickFixes(InspectionTree tree) {
    final TreePath[] treePaths = tree.getSelectionPaths();
    if (treePaths == null) return false;
    for (TreePath selectionPath : treePaths) {
      if (!TreeUtil.treeNodeTraverser((TreeNode)selectionPath.getLastPathComponent())
        .traverse(TreeTraversal.PRE_ORDER_DFS)
        .processEach(node -> {
        if (!((InspectionTreeNode) node).isValid()) return true;
        if (node instanceof ProblemDescriptionNode) {
          ProblemDescriptionNode problemDescriptionNode = (ProblemDescriptionNode)node;
          if (!problemDescriptionNode.isQuickFixAppliedFromView()) {
            final CommonProblemDescriptor descriptor = problemDescriptionNode.getDescriptor();
            final QuickFix[] fixes = descriptor != null ? descriptor.getFixes() : null;
            return fixes == null || fixes.length == 0;
          }
        }
        return true;
      })) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public abstract QuickFixAction[] getCommonQuickFixes(@NotNull InspectionToolWrapper toolWrapper, @NotNull InspectionTree tree);

  @NotNull
  public QuickFixAction[] getPartialQuickFixes(@NotNull InspectionToolWrapper toolWrapper, @NotNull InspectionTree tree) {
    GlobalInspectionContextImpl context = tree.getContext();
    InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
    CommonProblemDescriptor[] descriptors = tree.getSelectedDescriptors();
    Map<String, FixAndOccurrences> result = new THashMap<>();
    for (CommonProblemDescriptor d : descriptors) {
      QuickFix[] fixes = d.getFixes();
      if (fixes == null || fixes.length == 0) continue;
      for (QuickFix fix : fixes) {
        String familyName = fix.getFamilyName();
        FixAndOccurrences fixAndOccurrences = result.get(familyName);
        if (fixAndOccurrences == null) {
          LocalQuickFixWrapper localQuickFixWrapper = new LocalQuickFixWrapper(fix, presentation.getToolWrapper());
          try {
            localQuickFixWrapper.setText(StringUtil.escapeMnemonics(fix.getFamilyName()));
          }
          catch (AbstractMethodError e) {
            //for plugin compatibility
            localQuickFixWrapper.setText("Name is not available");
          }
          fixAndOccurrences = new FixAndOccurrences(localQuickFixWrapper);
          result.put(familyName, fixAndOccurrences);
        } else {
          final LocalQuickFixWrapper quickFixAction = fixAndOccurrences.fix;
          checkFixClass(presentation, fix, quickFixAction);
        }
        fixAndOccurrences.occurrences++;
      }
    }

    return result
      .values()
      .stream()
      .filter(fixAndOccurrence -> fixAndOccurrence.occurrences != descriptors.length)
      .sorted(Comparator.comparingInt((FixAndOccurrences fixAndOccurrence) -> fixAndOccurrence.occurrences).reversed())
      .map(fixAndOccurrence -> {
        LocalQuickFixWrapper fix = fixAndOccurrence.fix;
        int occurrences = fixAndOccurrence.occurrences;
        fix.setText(fix.getText() + " (" + occurrences + " problem" + (occurrences == 1 ? "" : "s") + ")");
        return fix;
      })
      .toArray(QuickFixAction[]::new);
  }

  protected static void checkFixClass(InspectionToolPresentation presentation, QuickFix fix, LocalQuickFixWrapper quickFixAction) {
    Class class1 = getFixClass(fix);
    Class class2 = getFixClass(quickFixAction.getFix());
    if (!class1.equals(class2)) {
      String message = MessageFormat.format(
        "QuickFix-es with the same family name ({0}) should be the same class instances but actually are {1} and {2} instances. " +
        "Please assign reported exception for the inspection \"{3}\" (\"{4}\") developer.",
        fix.getFamilyName(), class1.getName(), class2.getName(), presentation.getToolWrapper().getTool().getClass(),
        presentation.getToolWrapper().getShortName());
      AssertionError error = new AssertionError(message);
      StreamEx.of(presentation.getProblemDescriptors()).select(ProblemDescriptorBase.class)
              .map(ProblemDescriptorBase::getCreationTrace).nonNull()
              .map(InspectionRVContentProvider::extractStackTrace).findFirst()
              .ifPresent(error::setStackTrace);
      LOG.error(message, error);
    }
  }

  private static StackTraceElement[] extractStackTrace(Throwable throwable) {
    // Remove top-of-stack frames which are common for different inspections,
    // leaving only inspection-specific frames
    Set<String> classes = ContainerUtil.newHashSet(ProblemDescriptorBase.class.getName(), InspectionManagerBase.class.getName(), ProblemsHolder.class.getName());

    return StreamEx.of(throwable.getStackTrace())
            .dropWhile(ste -> classes.contains(ste.getClassName()))
            .toArray(StackTraceElement.class);
  }

  public void appendToolNodeContent(@NotNull GlobalInspectionContextImpl context,
                                    @NotNull InspectionToolWrapper wrapper,
                                    @NotNull InspectionTreeNode parentNode,
                                    boolean showStructure,
                                    boolean groupBySeverity) {
    InspectionToolPresentation presentation = context.getPresentation(wrapper);
    Map<String, Set<RefEntity>> content = presentation.getContent();
    appendToolNodeContent(context, wrapper, parentNode, showStructure, groupBySeverity, content, entity -> {
      if (context.getUIOptions().FILTER_RESOLVED_ITEMS) {
        return presentation.isExcluded(entity) ? null : presentation.getProblemElements().get(entity);
      } else {
        CommonProblemDescriptor[] problems = ObjectUtils.notNull(presentation.getProblemElements().get(entity), CommonProblemDescriptor.EMPTY_ARRAY);
        CommonProblemDescriptor[] suppressedProblems = presentation.getSuppressedProblems(entity);
        CommonProblemDescriptor[] resolvedProblems = presentation.getResolvedProblems(entity);
        CommonProblemDescriptor[] result = new CommonProblemDescriptor[problems.length + suppressedProblems.length + resolvedProblems.length];
        System.arraycopy(problems, 0, result, 0, problems.length);
        System.arraycopy(suppressedProblems, 0, result, problems.length, suppressedProblems.length);
        System.arraycopy(resolvedProblems, 0, result, problems.length + suppressedProblems.length, resolvedProblems.length);
        return result;
      }
    });
  }

  public abstract void appendToolNodeContent(@NotNull GlobalInspectionContextImpl context,
                                             @NotNull InspectionToolWrapper wrapper,
                                             @NotNull InspectionTreeNode parentNode,
                                             final boolean showStructure,
                                             boolean groupBySeverity,
                                             @NotNull Map<String, Set<RefEntity>> contents,
                                             @NotNull Function<? super RefEntity, CommonProblemDescriptor[]> problems);

  protected abstract void appendDescriptor(@NotNull GlobalInspectionContextImpl context,
                                           @NotNull InspectionToolWrapper toolWrapper,
                                           @NotNull RefEntityContainer container,
                                           @NotNull InspectionTreeNode parent);

  public boolean isContentLoaded() {
    return true;
  }

  protected <T> void buildTree(@NotNull GlobalInspectionContextImpl context,
                               @NotNull Map<String, Set<T>> packageContents,
                               @NotNull InspectionToolWrapper toolWrapper,
                               @NotNull Function<? super T, ? extends RefEntityContainer<?>> computeContainer,
                               boolean showStructure,
                               final InspectionTreeNode parent,
                               InspectionTreeModel model) {
    MultiMap<String, RefEntityContainer> evaluatedDescriptors = MultiMap.create();
    for (Map.Entry<String, Set<T>> entry : packageContents.entrySet()) {
      String packageName = entry.getKey();
      for (T problemDescriptor : entry.getValue()) {
        RefEntityContainer<?> container = computeContainer.apply(problemDescriptor);
        evaluatedDescriptors.putValue(packageName, container);
        showStructure &= container.supportStructure();
      }
    }

    for (Map.Entry<String, Collection<RefEntityContainer>> entry : evaluatedDescriptors.entrySet()) {
      for (RefEntityContainer container : entry.getValue()) {
        InspectionTreeNode currentParent = parent;
        if (showStructure) {
          String packageName = entry.getKey();
          Module module = container.getModule(context.getProject());
          InspectionTreeNode moduleNode = module != null ? model.createModuleNode(module, parent) : null;
          InspectionTreeNode actualParent = moduleNode == null ? parent : moduleNode;
          currentParent = packageName == null ? actualParent : model.createPackageNode(packageName, actualParent);
        }
        InspectionToolPresentation presentation = context.getPresentation(toolWrapper);
        RefElementNode node = container.createNode(presentation,
                                                   model,
                                                   currentParent,
                                                   showStructure
                                                   || HighlightInfoType.UNUSED_SYMBOL_DISPLAY_NAME.equals(toolWrapper.getDisplayName())
                                                   || presentation.isDummy());
        appendDescriptor(context, toolWrapper, container, node);
      }
    }
  }

  @NotNull
  protected static QuickFixAction[] getCommonFixes(@NotNull InspectionToolPresentation presentation,
                                                   @NotNull CommonProblemDescriptor[] descriptors) {
    Map<String, LocalQuickFixWrapper> result = null;
    for (CommonProblemDescriptor d : descriptors) {
      QuickFix[] fixes = d.getFixes();
      if (fixes == null || fixes.length == 0) continue;
      if (result == null) {
        result = new HashMap<>();
        for (QuickFix fix : fixes) {
          if (fix == null) continue;
          result.put(fix.getFamilyName(), new LocalQuickFixWrapper(fix, presentation.getToolWrapper()));
        }
      }
      else {
        for (String familyName : new ArrayList<>(result.keySet())) {
          boolean isFound = false;
          for (QuickFix fix : fixes) {
            if (fix == null) continue;
            if (familyName.equals(fix.getFamilyName())) {
              isFound = true;
              final LocalQuickFixWrapper quickFixAction = result.get(fix.getFamilyName());
              checkFixClass(presentation, fix, quickFixAction);
              try {
                quickFixAction.setText(StringUtil.escapeMnemonics(fix.getFamilyName()));
              }
              catch (AbstractMethodError e) {
                //for plugin compatibility
                quickFixAction.setText("Name is not available");
              }
              break;
            }
          }
          if (!isFound) {
            result.remove(familyName);
            if (result.isEmpty()) {
              return QuickFixAction.EMPTY;
            }
          }
        }
      }
    }
    return result == null || result.isEmpty() ? QuickFixAction.EMPTY : result.values().toArray(QuickFixAction.EMPTY);
  }

  private static Class getFixClass(QuickFix fix) {
    return fix instanceof ActionClassHolder ? ((ActionClassHolder)fix).getActionClass() : fix.getClass();
  }

  private static class FixAndOccurrences {
    final LocalQuickFixWrapper fix;
    int occurrences;
    FixAndOccurrences(LocalQuickFixWrapper fix) {this.fix = fix;}
  }
}
