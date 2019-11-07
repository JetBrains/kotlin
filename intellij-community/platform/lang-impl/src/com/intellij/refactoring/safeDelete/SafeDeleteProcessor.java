// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.safeDelete;

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.lang.LanguageRefactoringSupport;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.listeners.RefactoringEventListener;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteCustomUsageInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceUsageInfo;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteUsageInfo;
import com.intellij.refactoring.util.NonCodeSearchDescriptionLocation;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageInfoFactory;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.usages.*;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author dsl
 */
public class SafeDeleteProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(SafeDeleteProcessor.class);
  private final PsiElement[] myElements;
  private boolean mySearchInCommentsAndStrings;
  private boolean mySearchNonJava;
  private boolean myPreviewNonCodeUsages = true;
  private Runnable myAfterRefactoringCallback;

  private SafeDeleteProcessor(Project project, @Nullable Runnable prepareSuccessfulCallback,
                              PsiElement[] elementsToDelete, boolean isSearchInComments, boolean isSearchNonJava) {
    super(project, prepareSuccessfulCallback);
    myElements = elementsToDelete;
    mySearchInCommentsAndStrings = isSearchInComments;
    mySearchNonJava = isSearchNonJava;
  }

  @Override
  @NotNull
  protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
    return new SafeDeleteUsageViewDescriptor(myElements);
  }

  private static boolean isInside(PsiElement place, PsiElement[] ancestors) {
    return isInside(place, Arrays.asList(ancestors));
  }

  private static boolean isInside(PsiElement place, Collection<? extends PsiElement> ancestors) {
    for (PsiElement element : ancestors) {
      if (isInside(place, element)) return true;
    }
    return false;
  }

  public static boolean isInside (PsiElement place, PsiElement ancestor) {
    if (ancestor instanceof PsiDirectoryContainer) {
      final PsiDirectory[] directories = ((PsiDirectoryContainer)ancestor).getDirectories(place.getResolveScope());
      for (PsiDirectory directory : directories) {
        if (isInside(place, directory)) return true;
      }
    }

    if (ancestor instanceof PsiFile) {
      for (PsiFile file : ((PsiFile)ancestor).getViewProvider().getAllFiles()) {
        if (PsiTreeUtil.isAncestor(file, place, false)) return true;
      }
    }

    boolean isAncestor = PsiTreeUtil.isAncestor(ancestor, place, false);
    if (!isAncestor && ancestor instanceof PsiNameIdentifierOwner) {
      final PsiElement nameIdentifier = ((PsiNameIdentifierOwner)ancestor).getNameIdentifier();
      if (nameIdentifier != null && !PsiTreeUtil.isAncestor(ancestor, nameIdentifier, true)) {
        isAncestor = PsiTreeUtil.isAncestor(nameIdentifier.getParent(), place, false);
      }
    }

    if (!isAncestor) {
      final InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(place.getProject());
      PsiLanguageInjectionHost host = injectedLanguageManager.getInjectionHost(place);
      while (host != null) {
        if (PsiTreeUtil.isAncestor(ancestor, host, false)) {
          isAncestor = true;
          break;
        }
        host = injectedLanguageManager.getInjectionHost(host);
      }
    }
    return isAncestor;
  }

  @Override
  @NotNull
  protected UsageInfo[] findUsages() {
    List<UsageInfo> usages = Collections.synchronizedList(new ArrayList<>());
    GlobalSearchScope searchScope = GlobalSearchScope.projectScope(myProject);
    for (PsiElement element : myElements) {
      boolean handled = false;
      for(SafeDeleteProcessorDelegate delegate: SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        if (delegate.handlesElement(element)) {
          final NonCodeUsageSearchInfo filter = delegate.findUsages(element, myElements, usages);
          if (filter != null) {
            for(PsiElement nonCodeUsageElement: filter.getElementsToSearch()) {
              addNonCodeUsages(nonCodeUsageElement, searchScope, usages,
                               filter.getInsideDeletedCondition(), mySearchNonJava, mySearchInCommentsAndStrings);
            }
          }
          handled = true;
          break;
        }
      }
      if (!handled && element instanceof PsiNamedElement) {
        findGenericElementUsages(element, usages, myElements);
        addNonCodeUsages(element, searchScope, usages, getDefaultInsideDeletedCondition(myElements), mySearchNonJava, mySearchInCommentsAndStrings);
      }
    }
    UsageInfo[] result = usages.toArray(UsageInfo.EMPTY_ARRAY);
    result = UsageViewUtil.removeDuplicatedUsages(result);
    Arrays.sort(result, (o1, o2) -> PsiUtilCore.compareElementsByPosition(o2.getElement(), o1.getElement()));
    return result;
  }

  public static Condition<PsiElement> getDefaultInsideDeletedCondition(final PsiElement[] elements) {
    return usage -> !(usage instanceof PsiFile) && isInside(usage, elements);
  }

  public static void findGenericElementUsages(@NotNull PsiElement element, List<? super UsageInfo> usages, PsiElement[] allElementsToDelete, SearchScope scope) {
    ReferencesSearch.search(element, scope).forEach(reference -> {
      final PsiElement refElement = reference.getElement();
      if (!isInside(refElement, allElementsToDelete)) {
        usages.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(refElement, element, false));
      }
      return true;
    });
  }

  public static void findGenericElementUsages(final PsiElement element, final List<? super UsageInfo> usages, final PsiElement[] allElementsToDelete) {
    findGenericElementUsages(element, usages, allElementsToDelete, element.getUseScope());
  }

  @Override
  protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
    UsageInfo[] usages = refUsages.get();
    ArrayList<String> conflicts = new ArrayList<>();

    for (PsiElement element : myElements) {
      for(SafeDeleteProcessorDelegate delegate: SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        if (delegate.handlesElement(element)) {
          Collection<String> foundConflicts = delegate instanceof SafeDeleteProcessorDelegateBase
                                              ? ((SafeDeleteProcessorDelegateBase)delegate).findConflicts(element, myElements, usages)
                                              : delegate.findConflicts(element, myElements);
          if (foundConflicts != null) {
            conflicts.addAll(foundConflicts);
          }
          break;
        }
      }
    }

    if (checkConflicts(usages, conflicts)) return false;

    UsageInfo[] preprocessedUsages = usages;
    for(SafeDeleteProcessorDelegate delegate: SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
      preprocessedUsages = delegate.preprocessUsages(myProject, preprocessedUsages);
      if (preprocessedUsages == null) return false;
    }

    HashSet<UsageInfo> diff = ContainerUtil.newHashSet(preprocessedUsages);
    diff.removeAll(Arrays.asList(usages));

    if (checkConflicts(diff.toArray(UsageInfo.EMPTY_ARRAY), new ArrayList<>())) return false;

    final UsageInfo[] filteredUsages = UsageViewUtil.removeDuplicatedUsages(preprocessedUsages);
    prepareSuccessful(); // dialog is always dismissed
    if(filteredUsages == null) {
      return false;
    }
    refUsages.set(filteredUsages);
    return true;
  }

  private boolean checkConflicts(UsageInfo[] usages, ArrayList<String> conflicts) {
    final HashMap<PsiElement,UsageHolder> elementsToUsageHolders = sortUsages(usages);
    final Collection<UsageHolder> usageHolders = elementsToUsageHolders.values();
    for (UsageHolder usageHolder : usageHolders) {
      if (usageHolder.hasUnsafeUsagesInCode()) {
        conflicts.add(usageHolder.getDescription());
      }
    }

    if (!conflicts.isEmpty()) {
      final RefactoringEventData conflictData = new RefactoringEventData();
      conflictData.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts);
      myProject.getMessageBus().syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).conflictsDetected("refactoring.safeDelete", conflictData);
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        if (!ConflictsInTestsException.isTestIgnore()) throw new ConflictsInTestsException(conflicts);
      }
      else {
        UnsafeUsagesDialog dialog = new UnsafeUsagesDialog(ArrayUtilRt.toStringArray(conflicts), myProject);
        if (!dialog.showAndGet()) {
          final int exitCode = dialog.getExitCode();
          prepareSuccessful(); // dialog is always dismissed;
          if (exitCode == UnsafeUsagesDialog.VIEW_USAGES_EXIT_CODE) {
            showUsages(Arrays.stream(usages)
                         .filter(usage -> usage instanceof SafeDeleteReferenceUsageInfo &&
                                          !((SafeDeleteReferenceUsageInfo)usage).isSafeDelete()).toArray(UsageInfo[]::new),
                       usages);
          }
          return true;
        }
        else {
          myPreviewNonCodeUsages = false;
        }
      }
    }
    return false;
  }

  private void showUsages(final UsageInfo[] conflictUsages, final UsageInfo[] usages) {
    UsageViewPresentation presentation = new UsageViewPresentation();
    presentation.setTabText("Safe Delete Conflicts");
    presentation.setTargetsNodeText(RefactoringBundle.message("attempting.to.delete.targets.node.text"));
    presentation.setShowReadOnlyStatusAsRed(true);
    presentation.setShowCancelButton(true);
    presentation.setCodeUsagesString(RefactoringBundle.message("safe.delete.conflict.title"));
    presentation.setUsagesInGeneratedCodeString(RefactoringBundle.message("references.found.in.generated.code"));
    presentation.setNonCodeUsagesString(RefactoringBundle.message("occurrences.found.in.comments.strings.and.non.java.files"));
    presentation.setUsagesString(RefactoringBundle.message("usageView.usagesText"));

    UsageViewManager manager = UsageViewManager.getInstance(myProject);
    final UsageView usageView = showUsages(conflictUsages, presentation, manager);
    usageView.addPerformOperationAction(new RerunSafeDelete(myProject, myElements, usageView),
                                        RefactoringBundle.message("retry.command"), null, RefactoringBundle.message("rerun.safe.delete"));
    usageView.addPerformOperationAction(() -> {
      UsageInfo[] preprocessedUsages = usages;
      for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        preprocessedUsages = delegate.preprocessUsages(myProject, preprocessedUsages);
        if (preprocessedUsages == null) return;
      }
      final UsageInfo[] filteredUsages = UsageViewUtil.removeDuplicatedUsages(preprocessedUsages);
      execute(filteredUsages);
    }, "Delete Anyway", RefactoringBundle.message("usageView.need.reRun"), RefactoringBundle.message("usageView.doAction"));
  }

  private UsageView showUsages(UsageInfo[] usages, UsageViewPresentation presentation, UsageViewManager manager) {
    for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
       if (delegate instanceof SafeDeleteProcessorDelegateBase) {
         final UsageView view = ((SafeDeleteProcessorDelegateBase)delegate).showUsages(usages, presentation, manager, myElements);
         if (view != null) return view;
       }
    }
    UsageTarget[] targets = new UsageTarget[myElements.length];
    for (int i = 0; i < targets.length; i++) {
      targets[i] = new PsiElement2UsageTargetAdapter(myElements[i]);
    }

    return manager.showUsages(targets,
                              UsageInfoToUsageConverter.convert(myElements, usages),
                              presentation
    );
  }

  public PsiElement[] getElements() {
    return myElements;
  }

  private static class RerunSafeDelete implements Runnable {
    final SmartPsiElementPointer[] myPointers;
    private final Project myProject;
    private final UsageView myUsageView;

    RerunSafeDelete(Project project, PsiElement[] elements, UsageView usageView) {
      myProject = project;
      myUsageView = usageView;
      myPointers = new SmartPsiElementPointer[elements.length];
      for (int i = 0; i < elements.length; i++) {
        PsiElement element = elements[i];
        myPointers[i] = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element);
      }
    }

    @Override
    public void run() {
      PsiDocumentManager.getInstance(myProject).commitAllDocuments();
      myUsageView.close();
      ArrayList<PsiElement> elements = new ArrayList<>();
      for (SmartPsiElementPointer pointer : myPointers) {
        final PsiElement element = pointer.getElement();
        if (element != null) {
          elements.add(element);
        }
      }
      if(!elements.isEmpty()) {
        SafeDeleteHandler.invoke(myProject, PsiUtilCore.toPsiElementArray(elements), true);
      }
    }
  }

  /**
   * @param usages
   * @return Map from elements to UsageHolders
   */
  private static HashMap<PsiElement,UsageHolder> sortUsages(@NotNull UsageInfo[] usages) {
    HashMap<PsiElement,UsageHolder> result = new HashMap<>();

    for (final UsageInfo usage : usages) {
      if (usage instanceof SafeDeleteUsageInfo) {
        final PsiElement referencedElement = ((SafeDeleteUsageInfo)usage).getReferencedElement();
        if (!result.containsKey(referencedElement)) {
          result.put(referencedElement, new UsageHolder(referencedElement, usages));
        }
      }
    }
    return result;
  }


  @Override
  protected void refreshElements(@NotNull PsiElement[] elements) {
    LOG.assertTrue(elements.length == myElements.length);
    System.arraycopy(elements, 0, myElements, 0, elements.length);
  }

  @Override
  protected boolean isPreviewUsages(@NotNull UsageInfo[] usages) {
    if(myPreviewNonCodeUsages && UsageViewUtil.reportNonRegularUsages(usages, myProject)) {
      return true;
    }

    return super.isPreviewUsages(filterToBeDeleted(usages));
  }

  private static UsageInfo[] filterToBeDeleted(UsageInfo[] infos) {
    ArrayList<UsageInfo> list = new ArrayList<>();
    for (UsageInfo info : infos) {
      if (!(info instanceof SafeDeleteReferenceUsageInfo) || ((SafeDeleteReferenceUsageInfo) info).isSafeDelete()) {
        list.add(info);
      }
    }
    return list.toArray(UsageInfo.EMPTY_ARRAY);
  }

  @Nullable
  @Override
  protected RefactoringEventData getBeforeData() {
    final RefactoringEventData beforeData = new RefactoringEventData();
    beforeData.addElements(myElements);
    return beforeData;
  }

  @Nullable
  @Override
  protected String getRefactoringId() {
    return "refactoring.safeDelete";
  }

  @Override
  protected void performRefactoring(@NotNull UsageInfo[] usages) {
    try {
      SmartPointerManager pointerManager = SmartPointerManager.getInstance(myProject);
      List<SmartPsiElementPointer<PsiElement>> pointers = ContainerUtil.map(myElements, pointerManager::createSmartPsiElementPointer);

      for (UsageInfo usage : usages) {
        if (usage instanceof SafeDeleteCustomUsageInfo) {
          ((SafeDeleteCustomUsageInfo) usage).performRefactoring();
        }
      }

      for (PsiElement element : myElements) {
        for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
          if (delegate.handlesElement(element)) {
            delegate.prepareForDeletion(element);
          }
        }
      }

      for (SmartPsiElementPointer<PsiElement> pointer : pointers) {
        PsiElement element = pointer.getElement();
        if (element != null) {
          element.delete();
        }
      }
      if (myAfterRefactoringCallback != null) myAfterRefactoringCallback.run();
    } catch (IncorrectOperationException e) {
      RefactoringUIUtil.processIncorrectOperation(myProject, e);
    }
  }

  private String calcCommandName() {
    return RefactoringBundle.message("safe.delete.command", RefactoringUIUtil.calculatePsiElementDescriptionList(myElements));
  }

  private String myCachedCommandName = null;
  @NotNull
  @Override
  protected String getCommandName() {
    if (myCachedCommandName == null) {
      myCachedCommandName = calcCommandName();
    }
    return myCachedCommandName;
  }


  public static void addNonCodeUsages(PsiElement element,
                                      SearchScope searchScope,
                                      List<? super UsageInfo> usages,
                                      @Nullable final Condition<? super PsiElement> insideElements,
                                      boolean searchNonJava,
                                      boolean searchInCommentsAndStrings) {
    UsageInfoFactory nonCodeUsageFactory = new UsageInfoFactory() {
      @Override
      public UsageInfo createUsageInfo(@NotNull PsiElement usage, int startOffset, int endOffset) {
        if (insideElements != null && insideElements.value(usage)) {
          return null;
        }
        return new SafeDeleteReferenceSimpleDeleteUsageInfo(usage, element, startOffset, endOffset, true, false);
      }
    };
    if (searchInCommentsAndStrings) {
      String stringToSearch = ElementDescriptionUtil.getElementDescription(element, NonCodeSearchDescriptionLocation.STRINGS_AND_COMMENTS);
      TextOccurrencesUtil.addUsagesInStringsAndComments(element, searchScope, stringToSearch, usages, nonCodeUsageFactory);
    }
    if (searchNonJava && searchScope instanceof GlobalSearchScope) {
      String stringToSearch = ElementDescriptionUtil.getElementDescription(element, NonCodeSearchDescriptionLocation.NON_JAVA);
      TextOccurrencesUtil.addTextOccurrences(element, stringToSearch, (GlobalSearchScope)searchScope, usages, nonCodeUsageFactory);
    }
  }

  @Override
  protected boolean isToBeChanged(@NotNull UsageInfo usageInfo) {
    if (usageInfo instanceof SafeDeleteReferenceUsageInfo) {
      return ((SafeDeleteReferenceUsageInfo)usageInfo).isSafeDelete() && super.isToBeChanged(usageInfo);
    }
    return super.isToBeChanged(usageInfo);
  }

  public static boolean validElement(@NotNull PsiElement element) {
    if (element instanceof PsiFile) return true;
    if (!element.isPhysical()) return false;
    final RefactoringSupportProvider provider = LanguageRefactoringSupport.INSTANCE.forContext(element);
    return provider != null && provider.isSafeDeleteAvailable(element);
  }

  public static SafeDeleteProcessor createInstance(Project project, @Nullable Runnable prepareSuccessfulCallback,
                                                   PsiElement[] elementsToDelete, boolean isSearchInComments, boolean isSearchNonJava) {
    return new SafeDeleteProcessor(project, prepareSuccessfulCallback, elementsToDelete, isSearchInComments, isSearchNonJava);
  }

  public static SafeDeleteProcessor createInstance(Project project, @Nullable Runnable prepareSuccessfulCallBack,
                                                   PsiElement[] elementsToDelete, boolean isSearchInComments, boolean isSearchNonJava,
                                                   boolean askForAccessors) {
    ArrayList<PsiElement> elements = new ArrayList<>(Arrays.asList(elementsToDelete));
    Set<PsiElement> elementsToDeleteSet = ContainerUtil.set(elementsToDelete);

    for (PsiElement psiElement : elementsToDelete) {
      for(SafeDeleteProcessorDelegate delegate: SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        if (delegate.handlesElement(psiElement)) {
          Collection<PsiElement> addedElements = delegate.getAdditionalElementsToDelete(psiElement, elementsToDeleteSet, askForAccessors);
          if (addedElements != null) {
            elements.addAll(addedElements);
          }
          break;
        }
      }
    }

    return new SafeDeleteProcessor(project, prepareSuccessfulCallBack,
                                   PsiUtilCore.toPsiElementArray(elements),
                                   isSearchInComments, isSearchNonJava);
  }

  public boolean isSearchInCommentsAndStrings() {
    return mySearchInCommentsAndStrings;
  }

  public void setSearchInCommentsAndStrings(boolean searchInCommentsAndStrings) {
    mySearchInCommentsAndStrings = searchInCommentsAndStrings;
  }

  public boolean isSearchNonJava() {
    return mySearchNonJava;
  }

  public void setSearchNonJava(boolean searchNonJava) {
    mySearchNonJava = searchNonJava;
  }

  @Override
  protected boolean skipNonCodeUsages() {
    return true;
  }

  public void setAfterRefactoringCallback(Runnable afterRefactoringCallback) {
    myAfterRefactoringCallback = afterRefactoringCallback;
  }
}
