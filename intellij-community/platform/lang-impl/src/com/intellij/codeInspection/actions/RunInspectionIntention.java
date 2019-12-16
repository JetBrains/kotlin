// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.analysis.dialog.ModelScopeItem;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.*;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class RunInspectionIntention implements IntentionAction, HighPriorityAction {
  private final static Logger LOG = Logger.getInstance(RunInspectionIntention.class);

  private final String myShortName;

  public RunInspectionIntention(final HighlightDisplayKey key) {
    myShortName = key.toString();
  }

  @Override
  @NotNull
  public String getText() {
    return InspectionsBundle.message("run.inspection.on.file.intention.text");
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return LocalInspectionToolWrapper.findTool2RunInBatch(project, file, myShortName) != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final Module module = file != null ? ModuleUtilCore.findModuleForPsiElement(file) : null;
    AnalysisScope analysisScope = new AnalysisScope(project);
    if (file != null) {
      PsiFile topLevelFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(file);
      final VirtualFile virtualFile = topLevelFile.getVirtualFile();
      if (file.isPhysical() && virtualFile != null && virtualFile.isInLocalFileSystem()) {
        analysisScope = new AnalysisScope(topLevelFile);
      }
    }

    selectScopeAndRunInspection(myShortName, analysisScope, module, file, project);
  }

  public static void selectScopeAndRunInspection(@NotNull String toolShortName,
                                                 @NotNull AnalysisScope customScope,
                                                 @Nullable Module module,
                                                 @Nullable PsiElement context,
                                                 @NotNull Project project) {
    List<ModelScopeItem> items = BaseAnalysisActionDialog.standardItems(project, customScope, module, context);
    final BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(
      AnalysisScopeBundle.message("specify.analysis.scope", InspectionsBundle.message("inspection.action.title")),
      AnalysisScopeBundle.message("analysis.scope.title", InspectionsBundle.message("inspection.action.noun")), project,
      items, AnalysisUIOptions.getInstance(project), true);
    if (!dlg.showAndGet()) {
      return;
    }
    final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
    customScope = dlg.getScope(uiOptions, customScope, project, module);
    final InspectionToolWrapper wrapper = LocalInspectionToolWrapper.findTool2RunInBatch(project, context, toolShortName);
    LOG.assertTrue(wrapper != null, "Can't find tool with name = \"" + toolShortName + "\"");
    rerunInspection(wrapper, (InspectionManagerEx)InspectionManager.getInstance(project), customScope, context);
  }

  public static void rerunInspection(@NotNull InspectionToolWrapper toolWrapper,
                                     @NotNull InspectionManagerEx managerEx,
                                     @NotNull AnalysisScope scope,
                                     @Nullable PsiElement psiElement) {
    GlobalInspectionContextImpl inspectionContext = createContext(toolWrapper, managerEx, psiElement);
    inspectionContext.doInspections(scope);
  }

  @NotNull
  public static GlobalInspectionContextImpl createContext(@NotNull InspectionToolWrapper toolWrapper,
                                                          @NotNull InspectionManagerEx managerEx,
                                                          @Nullable PsiElement psiElement) {
    final InspectionProfileImpl model = createProfile(toolWrapper, managerEx, psiElement);
    final GlobalInspectionContextImpl inspectionContext = managerEx.createNewGlobalContext();
    inspectionContext.setExternalProfile(model);
    return inspectionContext;
  }

  @NotNull
  public static InspectionProfileImpl createProfile(@NotNull InspectionToolWrapper toolWrapper,
                                                    @NotNull InspectionManagerEx managerEx,
                                                    @Nullable PsiElement psiElement) {
    InspectionProfileImpl rootProfile = InspectionProfileManager.getInstance(managerEx.getProject()).getCurrentProfile();
    LinkedHashSet<InspectionToolWrapper<?, ?>> allWrappers = new LinkedHashSet<>();
    allWrappers.add(toolWrapper);
    rootProfile.collectDependentInspections(toolWrapper, allWrappers, managerEx.getProject());
    List<InspectionToolWrapper<?, ?>> toolWrappers = allWrappers.size() == 1 ? Collections.singletonList(allWrappers.iterator().next()) : new ArrayList<>(allWrappers);
    InspectionProfileImpl model = InspectionProfileKt.createSimple(toolWrapper.getDisplayName(), managerEx.getProject(), toolWrappers);
    try {
      Element element = new Element("toCopy");
      for (InspectionToolWrapper wrapper : toolWrappers) {
        wrapper.getTool().writeSettings(element);
        InspectionToolWrapper tw = psiElement == null ? model.getInspectionTool(wrapper.getShortName(), managerEx.getProject())
                                                      : model.getInspectionTool(wrapper.getShortName(), psiElement);
        tw.getTool().readSettings(element);
      }
    }
    catch (WriteExternalException | InvalidDataException ignored) {
    }
    model.setSingleTool(toolWrapper.getShortName());
    return model;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
