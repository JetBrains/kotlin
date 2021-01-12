// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.analysis.dialog.ModelScopeItem;
import com.intellij.codeInsight.CodeInsightBundle;
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

public final class RunInspectionIntention implements IntentionAction, HighPriorityAction {
  private static final Logger LOG = Logger.getInstance(RunInspectionIntention.class);

  private final String myShortName;

  public RunInspectionIntention(@NotNull HighlightDisplayKey key) {
    myShortName = key.toString();
  }

  @Override
  public @NotNull String getText() {
    return InspectionsBundle.message("run.inspection.on.file.intention.text");
  }

  @Override
  public @NotNull String getFamilyName() {
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
      CodeInsightBundle.message("specify.analysis.scope", InspectionsBundle.message("inspection.action.title")),
      CodeInsightBundle.message("analysis.scope.title", InspectionsBundle.message("inspection.action.noun")), project,
      items, AnalysisUIOptions.getInstance(project), true);
    if (!dlg.showAndGet()) {
      return;
    }
    customScope = dlg.getScope(customScope);
    InspectionToolWrapper<?, ?> wrapper = LocalInspectionToolWrapper.findTool2RunInBatch(project, context, toolShortName);
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

  public static @NotNull GlobalInspectionContextImpl createContext(@NotNull InspectionToolWrapper toolWrapper,
                                                                   @NotNull InspectionManagerEx managerEx,
                                                                   @Nullable PsiElement psiElement) {
    final InspectionProfileImpl model = createProfile(toolWrapper, managerEx, psiElement);
    final GlobalInspectionContextImpl inspectionContext = managerEx.createNewGlobalContext();
    inspectionContext.setExternalProfile(model);
    return inspectionContext;
  }

  public static @NotNull InspectionProfileImpl createProfile(@NotNull InspectionToolWrapper toolWrapper,
                                                             @NotNull InspectionManagerEx managerEx,
                                                             @Nullable PsiElement psiElement) {
    final Project project = managerEx.getProject();
    InspectionProfileImpl rootProfile = InspectionProfileManager.getInstance(project).getCurrentProfile();
    LinkedHashSet<InspectionToolWrapper<?, ?>> allWrappers = new LinkedHashSet<>();
    allWrappers.add(toolWrapper);
    rootProfile.collectDependentInspections(toolWrapper, allWrappers, project);
    List<InspectionToolWrapper<?, ?>> toolWrappers = allWrappers.size() == 1 ? Collections.singletonList(allWrappers.iterator().next()) : new ArrayList<>(allWrappers);
    InspectionProfileImpl model = new InspectionProfileImpl(toolWrapper.getDisplayName(), new InspectionToolsSupplier.Simple(toolWrappers), rootProfile);
    for (InspectionToolWrapper wrapper : toolWrappers) {
      model.enableTool(wrapper.getShortName(), project);
    }
    try {
      Element element = new Element("toCopy");
      for (InspectionToolWrapper wrapper : toolWrappers) {
        wrapper.getTool().writeSettings(element);
        InspectionToolWrapper tw = psiElement == null ? model.getInspectionTool(wrapper.getShortName(), project)
                                                      : model.getInspectionTool(wrapper.getShortName(), psiElement);
        assert tw != null;
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
