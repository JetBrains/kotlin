// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.ex.*;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefManagerImpl;
import com.intellij.codeInspection.ui.SingleCheckboxOptionsPanel;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.*;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.BidirectionalMap;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class RedundantSuppressInspection extends GlobalSimpleInspectionTool {
  private static final Logger LOG = Logger.getInstance(RedundantSuppressInspection.class);
  public static final String SHORT_NAME = "RedundantSuppression";
  public boolean IGNORE_ALL;
  private BidirectionalMap<String, QuickFix> myQuickFixes;

  @Override
  @NotNull
  public String getGroupDisplayName() {
    return GroupNames.DECLARATION_REDUNDANCY;
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return InspectionsBundle.message("inspection.redundant.suppression.name");
  }

  @Override
  @NotNull
  @NonNls
  public String getShortName() {
    return SHORT_NAME;
  }

  @Override
  public JComponent createOptionsPanel() {
    return new SingleCheckboxOptionsPanel("Ignore '@SuppressWarning(\"ALL\")'", this, "IGNORE_ALL");
  }

  @Override
  public void writeSettings(@NotNull Element node) throws WriteExternalException {
    if (IGNORE_ALL) {
      super.writeSettings(node);
    }
  }

  @Override
  public void checkFile(@NotNull PsiFile file,
                        @NotNull InspectionManager manager,
                        @NotNull ProblemsHolder problemsHolder,
                        @NotNull GlobalInspectionContext globalContext,
                        @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor) {
    InspectionSuppressor extension = LanguageInspectionSuppressors.INSTANCE.forLanguage(file.getLanguage());
    if (!(extension instanceof RedundantSuppressionDetector)) return;
    final CommonProblemDescriptor[] descriptors = checkElement(file, (RedundantSuppressionDetector)extension, manager);
    for (CommonProblemDescriptor descriptor : descriptors) {
      if (descriptor instanceof ProblemDescriptor) {
        final PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
        if (psiElement != null) {
          final PsiElement member = globalContext.getRefManager().getContainerElement(psiElement);
          final RefElement reference = globalContext.getRefManager().getReference(member);
          if (reference != null) {
            problemDescriptionsProcessor.addProblemElement(reference, descriptor);
          }
          else {
            problemsHolder.registerProblem(psiElement, descriptor.getDescriptionTemplate());
          }
          continue;
        }
      }
      problemsHolder.registerProblem(file, descriptor.getDescriptionTemplate());
    }
  }

  @NotNull
  public ProblemDescriptor[] checkElement(@NotNull final PsiFile psiElement,
                                          RedundantSuppressionDetector extension,
                                          @NotNull final InspectionManager manager) {
    final Map<PsiElement, Collection<String>> suppressedScopes = new THashMap<>();
    psiElement.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        collectSuppressions(element, suppressedScopes, IGNORE_ALL, extension);
      }
    });

    if (suppressedScopes.values().isEmpty()) return ProblemDescriptor.EMPTY_ARRAY;
    // have to visit all file from scratch since inspections can be written in any pervasive way including checkFile() overriding
    Map<InspectionToolWrapper<?, ?>, String> suppressedTools = new THashMap<>();
    InspectionToolWrapper<?, ?>[] toolWrappers = getInspectionTools(psiElement, manager);
    for (Collection<String> ids : suppressedScopes.values()) {
      for (Iterator<String> iterator = ids.iterator(); iterator.hasNext(); ) {
        String suppressId = iterator.next().trim();
        List<InspectionToolWrapper<?, ?>> reportingWrappers = findReportingTools(toolWrappers, suppressId);
        if (reportingWrappers.isEmpty()) {
          iterator.remove();
        }
        else {
          for (InspectionToolWrapper<?, ?> toolWrapper : reportingWrappers) {
            suppressedTools.put(toolWrapper, suppressId);
          }
        }
      }
    }

    PsiFile file = psiElement.getContainingFile();
    final AnalysisScope scope = new AnalysisScope(file);

    final GlobalInspectionContextBase globalContext = createContext(file);
    globalContext.setCurrentScope(scope);
    final RefManagerImpl refManager = (RefManagerImpl)globalContext.getRefManager();
    refManager.inspectionReadActionStarted();
    final List<ProblemDescriptor> result;
    try {
      result = new ArrayList<>();
      for (InspectionToolWrapper<?, ?> toolWrapper : suppressedTools.keySet()) {
        String toolId = suppressedTools.get(toolWrapper);
        toolWrapper.initialize(globalContext);
        final Collection<CommonProblemDescriptor> descriptors;
        if (toolWrapper instanceof LocalInspectionToolWrapper) {
          LocalInspectionToolWrapper local = (LocalInspectionToolWrapper)toolWrapper;
          if (local.isUnfair()) continue; //cant't work with passes other than LocalInspectionPass
          List<ProblemDescriptor> results = local.getTool().processFile(file, manager);
          descriptors = new ArrayList<>(results);
        }
        else if (toolWrapper instanceof GlobalInspectionToolWrapper) {
          final GlobalInspectionToolWrapper global = (GlobalInspectionToolWrapper)toolWrapper;
          GlobalInspectionTool globalTool = global.getTool();
          //when graph is needed, results probably depend on outer files so absence of results on one file (in current context) doesn't guarantee anything
          if (globalTool.isGraphNeeded()) continue;
          descriptors = new ArrayList<>(InspectionEngine.runInspectionOnFile(file, global, globalContext));
        }
        else {
          continue;
        }
        for (PsiElement suppressedScope : suppressedScopes.keySet()) {
          Collection<String> suppressedIds = suppressedScopes.get(suppressedScope);
          if (!suppressedIds.contains(toolId)) continue;
          for (CommonProblemDescriptor descriptor : descriptors) {
            if (!(descriptor instanceof ProblemDescriptor)) continue;
            PsiElement element = ((ProblemDescriptor)descriptor).getPsiElement();
            if (element == null) continue;
            PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(element.getProject()).getInjectionHost(element);
            if (extension.isSuppressionFor(suppressedScope, ObjectUtils.notNull(host, element), toolId)) {
              suppressedIds.remove(toolId);
              break;
            }
          }
        }
      }
      for (PsiElement suppressedScope : suppressedScopes.keySet()) {
        Collection<String> suppressedIds = suppressedScopes.get(suppressedScope);
        for (String toolId : suppressedIds) {
          PsiElement documentedElement = globalContext.getRefManager().getContainerElement(suppressedScope);
          if (documentedElement != null && documentedElement.isValid()) {
            if (myQuickFixes == null) myQuickFixes = new BidirectionalMap<>();
            String key = toolId + ";" + suppressedScope.getLanguage().getID();
            QuickFix fix = myQuickFixes.get(key);
            if (fix == null) {
              fix = createQuickFix(key);
              myQuickFixes.put(key, fix);
            }
            PsiElement identifier;
            if (suppressedScope instanceof PsiNameIdentifierOwner && suppressedScope == documentedElement) {
              identifier = ObjectUtils.notNull(((PsiNameIdentifierOwner)suppressedScope).getNameIdentifier(), suppressedScope);
            }
            else {
              identifier = suppressedScope;
            }
            result.add(
              manager.createProblemDescriptor(identifier, InspectionsBundle.message("inspection.redundant.suppression.description"), (LocalQuickFix)fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                              false));
          }
        }
      }
    }
    finally {
      refManager.inspectionReadActionFinished();
      globalContext.close(true);
    }
    return result.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static List<InspectionToolWrapper<?, ?>> findReportingTools(InspectionToolWrapper<?, ?>[] toolWrappers, String suppressedId) {
    List<InspectionToolWrapper<?, ?>> wrappers = Collections.emptyList();
    String mergedToolName = InspectionElementsMerger.getMergedToolName(suppressedId);
    for (InspectionToolWrapper<?, ?> toolWrapper : toolWrappers) {
      String toolWrapperShortName = toolWrapper.getShortName();
      String alternativeID = toolWrapper.getTool().getAlternativeID();
      if (toolWrapper instanceof LocalInspectionToolWrapper &&
          (((LocalInspectionToolWrapper)toolWrapper).getTool().getID().equals(suppressedId) ||
           suppressedId.equals(alternativeID) ||
           toolWrapperShortName.equals(mergedToolName))) {
        if (!((LocalInspectionToolWrapper)toolWrapper).isUnfair()) {
          if (wrappers.isEmpty()) wrappers = new ArrayList<>();
          wrappers.add(toolWrapper);
        }
      }
      else if (toolWrapperShortName.equals(suppressedId) || toolWrapperShortName.equals(mergedToolName) || suppressedId.equals(alternativeID)) {
        //ignore global unused as it won't be checked anyway
        if (toolWrapper instanceof LocalInspectionToolWrapper ||
            toolWrapper instanceof GlobalInspectionToolWrapper && !((GlobalInspectionToolWrapper)toolWrapper).getTool().isGraphNeeded()) {
          if (wrappers.isEmpty()) wrappers = new ArrayList<>();
          wrappers.add(toolWrapper);
        }
      }
    }
    return wrappers;
  }

  private static boolean collectSuppressions(@NotNull PsiElement element,
                                             Map<PsiElement, Collection<String>> suppressedScopes,
                                             boolean ignoreAll,
                                             RedundantSuppressionDetector suppressor) {
    String idsString = suppressor.getSuppressionIds(element);
    if (idsString != null && !idsString.isEmpty()) {
      List<String> ids = new ArrayList<>();
      StringUtil.tokenize(idsString, "[, ]").forEach(ids::add);
      boolean isSuppressAll = ids.stream().anyMatch(id -> id.equalsIgnoreCase(SuppressionUtil.ALL));
      if (ignoreAll && isSuppressAll) {
        return false;
      }
      Collection<String> suppressed = suppressedScopes.get(element);
      if (suppressed == null) {
        suppressed = ids;
      }
      else {
        for (String id : ids) {
          if (!suppressed.contains(id)) {
            suppressed.add(id);
          }
        }
      }
      suppressedScopes.put(element, suppressed);
      return isSuppressAll;
    }
    return false;
  }
  
  public LocalInspectionTool createLocalTool(RedundantSuppressionDetector suppressor,
                                             Map<String, Set<PsiElement>> toolToSuppressScopes,
                                             Set<String> activeTools) {
    return new LocalRedundantSuppressionInspection(suppressor, activeTools, toolToSuppressScopes);
  }

  private static QuickFix<ProblemDescriptor> createQuickFix(String key) {
    String[] toolAndLang = key.split(";");
    Language language = toolAndLang.length < 2 ? null : Language.findLanguageByID(toolAndLang[1]);
    if (language == null) return null;
    InspectionSuppressor suppressor = LanguageInspectionSuppressors.INSTANCE.forLanguage(language);
    return suppressor instanceof RedundantSuppressionDetector
           ? ((RedundantSuppressionDetector)suppressor).createRemoveRedundantSuppressionFix(toolAndLang[0]) : null;
  }

  @NotNull
  protected InspectionToolWrapper<?, ?>[] getInspectionTools(PsiElement psiElement, @NotNull InspectionManager manager) {
    String currentProfileName = ((InspectionManagerBase)manager).getCurrentProfile();
    InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(manager.getProject());
    InspectionProfileImpl usedProfile = profileManager.getProfile(currentProfileName, false);
    return ObjectUtils.notNull(usedProfile, profileManager.getCurrentProfile()).getInspectionTools(psiElement);
  }

  @Override
  @Nullable
  public QuickFix getQuickFix(final String hint) {
    return myQuickFixes != null ? myQuickFixes.get(hint) : createQuickFix(hint);
  }

  @Override
  @Nullable
  public String getHint(@NotNull final QuickFix fix) {
    if (myQuickFixes != null) {
      final List<String> list = myQuickFixes.getKeysByValue(fix);
      if (list != null) {
        LOG.assertTrue(list.size() == 1);
        return list.get(0);
      }
    }
    return null;
  }

  @Override
  public boolean worksInBatchModeOnly() {
    return false;
  }

  protected GlobalInspectionContextBase createContext(PsiFile file) {
    final InspectionManager inspectionManagerEx = InspectionManager.getInstance(file.getProject());
    return (GlobalInspectionContextBase)inspectionManagerEx.createNewGlobalContext();
  }

  private class LocalRedundantSuppressionInspection extends LocalInspectionTool implements UnfairLocalInspectionTool {
    private final RedundantSuppressionDetector mySuppressor;
    private final Set<String> myActiveTools;
    private final Map<String, Set<PsiElement>> myToolToSuppressScopes;

    private LocalRedundantSuppressionInspection(RedundantSuppressionDetector suppressor,
                                                Set<String> activeTools,
                                                Map<String, Set<PsiElement>> toolToSuppressScopes) {
      mySuppressor = suppressor;
      myActiveTools = activeTools;
      myToolToSuppressScopes = toolToSuppressScopes;
    }

    @NotNull
    @Override
    public String getShortName() {
      return SHORT_NAME;
    }

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
      return RedundantSuppressInspection.this.getDisplayName();
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
      return new PsiElementVisitor() {

        @Override
        public void visitElement(PsiElement element) {
          super.visitElement(element);
          HashMap<PsiElement, Collection<String>> scopes = new HashMap<>();
          boolean suppressAll = collectSuppressions(element, scopes, IGNORE_ALL, mySuppressor);
          if (suppressAll) {
            for (String suppressId : myActiveTools) {
              if (isSuppressedFor(element, suppressId, myToolToSuppressScopes.get(suppressId))) {
                return;
              }
            }
            TextRange range = mySuppressor.getHighlightingRange(element, SuppressionUtil.ALL);
            String allSuppression = element.getText().substring(range.getStartOffset(), range.getEndOffset());
            holder.registerProblem(element, range,
                                   InspectionsBundle.message("inspection.redundant.suppression.description"),
                                   mySuppressor.createRemoveRedundantSuppressionFix(allSuppression));
            return;
          }
          Collection<String> suppressIds = scopes.get(element);
          if (suppressIds != null) {
            for (String suppressId : suppressIds) {
              if (myActiveTools.contains(suppressId) &&
                  !isSuppressedFor(element, suppressId, myToolToSuppressScopes.get(suppressId)) &&
                  //suppression in local pass is intentionally disabled to pass ALL
                  !SuppressionUtil.inspectionResultSuppressed(element, LocalRedundantSuppressionInspection.this)) {
                holder.registerProblem(element, mySuppressor.getHighlightingRange(element, suppressId),
                                       InspectionsBundle.message("inspection.redundant.suppression.description"),
                                       mySuppressor.createRemoveRedundantSuppressionFix(suppressId));
              }
            }
          }
        }

        private boolean isSuppressedFor(PsiElement element, String suppressId, Set<? extends PsiElement> suppressedPlaces) {
          return suppressedPlaces != null && 
                 suppressedPlaces.stream().anyMatch(place -> mySuppressor.isSuppressionFor(element, place, suppressId));
        }
      };
    }
  }
}
