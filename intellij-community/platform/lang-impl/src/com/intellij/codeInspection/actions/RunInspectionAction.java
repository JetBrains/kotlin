// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.actions;

import com.intellij.CommonBundle;
import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.analysis.dialog.ModelScopeItem;
import com.intellij.codeInspection.CleanupLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.ChooseByNameFilter;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * @author Konstantin Bulenkov
 */
public class RunInspectionAction extends GotoActionBase implements DataProvider {
  private static final Logger LOGGER = Logger.getInstance(RunInspectionAction.class);
  private final String myPredefinedText;

  @SuppressWarnings("unused")
  public RunInspectionAction() {
    this(null);
  }

  public RunInspectionAction(String predefinedText) {
    myPredefinedText = predefinedText;
    getTemplatePresentation().setText(IdeBundle.message("goto.inspection.action.text"));
  }

  @Override
  protected void gotoActionPerformed(@NotNull final AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
    final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.inspection");

    final GotoInspectionModel model = new GotoInspectionModel(project);
    showNavigationPopup(e, model, new GotoActionCallback<Object>() {
      @Override
      protected ChooseByNameFilter<Object> createFilter(@NotNull ChooseByNamePopup popup) {
        popup.setSearchInAnyPlace(true);
        return super.createFilter(popup);
      }

      @Override
      public void elementChosen(ChooseByNamePopup popup, final Object element) {
        ApplicationManager.getApplication().invokeLater(
          () -> runInspection(project, (((InspectionElement)element)).getToolWrapper().getShortName(), virtualFile, psiElement, psiFile));
      }
    }, false);
  }

  @Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    return PlatformDataKeys.PREDEFINED_TEXT.is(dataId) ? myPredefinedText : null;
  }

  public static void runInspection(final @NotNull Project project,
                                   @NotNull String shortName,
                                   @Nullable VirtualFile virtualFile,
                                   PsiElement psiElement,
                                   PsiFile psiFile) {
    final PsiElement element = psiFile == null ? psiElement : psiFile;
    final InspectionProfile currentProfile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
    final InspectionToolWrapper toolWrapper = element != null ? currentProfile.getInspectionTool(shortName, element)
                                                              : currentProfile.getInspectionTool(shortName, project);
    LOGGER.assertTrue(toolWrapper != null, "Missed inspection: " + shortName);

    final InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final Module module = virtualFile != null ? ModuleUtilCore.findModuleForFile(virtualFile, project) : null;

    AnalysisScope analysisScope = null;
    if (psiFile != null) {
      analysisScope = new AnalysisScope(psiFile);
    }
    else {
      if (virtualFile != null && virtualFile.isDirectory()) {
        final PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile);
        if (psiDirectory != null) {
          analysisScope = new AnalysisScope(psiDirectory);
        }
      }
      if (analysisScope == null && virtualFile != null) {
        analysisScope = new AnalysisScope(project, Collections.singletonList(virtualFile));
      }
      if (analysisScope == null) {
        analysisScope = new AnalysisScope(project);
      }
    }

    final AnalysisUIOptions options = AnalysisUIOptions.getInstance(project);
    final FileFilterPanel fileFilterPanel = new FileFilterPanel();
    fileFilterPanel.init(options);

    final AnalysisScope initialAnalysisScope = analysisScope;
    List<ModelScopeItem> items = BaseAnalysisActionDialog.standardItems(project, analysisScope, module, psiElement);
    final BaseAnalysisActionDialog dialog = new BaseAnalysisActionDialog("Run '" + toolWrapper.getDisplayName() + "'",
                                                                         AnalysisScopeBundle.message("analysis.scope.title", InspectionsBundle
                                                                           .message("inspection.action.noun")), project,
                                                                         items, options, true) {

      private InspectionToolWrapper myUpdatedSettingsToolWrapper;

      @Nullable
      @Override
      protected JComponent getAdditionalActionSettings(Project project) {
        final JPanel fileFilter = fileFilterPanel.getPanel();
        if (toolWrapper.getTool().createOptionsPanel() != null) {
          JPanel additionPanel = new JPanel();
          additionPanel.setLayout(new GridBagLayout());
          additionPanel.add(fileFilter, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));
          myUpdatedSettingsToolWrapper = copyToolWithSettings(toolWrapper);//new InheritOptionsForToolPanel(toolWrapper.getShortName(), project);
          additionPanel.add(new TitledSeparator(IdeBundle.message("goto.inspection.action.choose.inherit.settings.from")), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));
          JComponent optionsPanel = myUpdatedSettingsToolWrapper.getTool().createOptionsPanel();
          LOGGER.assertTrue(optionsPanel != null);
          GridBagConstraints constraints =
            new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0);
          if (UIUtil.hasScrollPane(optionsPanel)) {
            additionPanel.add(optionsPanel, constraints);
          }
          else {
            additionPanel.add(ScrollPaneFactory.createScrollPane(optionsPanel, SideBorder.NONE), constraints);
          }
          return additionPanel;
        } else {
          return fileFilter;
        }
      }

      @NotNull
      @Override
      public AnalysisScope getScope(@NotNull AnalysisScope defaultScope) {
        final AnalysisScope scope = super.getScope(defaultScope);
        final GlobalSearchScope filterScope = fileFilterPanel.getSearchScope();
        if (filterScope == null) {
          return scope;
        }
        scope.setFilter(filterScope);
        return scope;
      }

      private AnalysisScope getScope() {
        return getScope(initialAnalysisScope);
      }

      private InspectionToolWrapper getToolWrapper() {
        return myUpdatedSettingsToolWrapper == null ? toolWrapper : myUpdatedSettingsToolWrapper;
      }

      @NotNull
      @Override
      protected Action[] createActions() {
        final List<Action> actions = new ArrayList<>();
        final boolean hasFixAll = toolWrapper.getTool() instanceof CleanupLocalInspectionTool;
        actions.add(new AbstractAction(hasFixAll ? AnalysisScopeBundle.message("action.analyze.verb")
                                                 : CommonBundle.getOkButtonText()) {
          {
            putValue(DEFAULT_ACTION, Boolean.TRUE);
          }
          @Override
          public void actionPerformed(ActionEvent e) {
            AnalysisScope scope = getScope();
            InspectionToolWrapper wrapper = getToolWrapper();
            DumbService.getInstance(project).smartInvokeLater(() -> RunInspectionIntention.rerunInspection(wrapper, managerEx, scope, null));
            close(DialogWrapper.OK_EXIT_CODE);
          }
        });
        if (hasFixAll) {
          actions.add(new AbstractAction("Fix All") {
            @Override
            public void actionPerformed(ActionEvent e) {
              InspectionToolWrapper wrapper = getToolWrapper();
              InspectionProfileImpl cleanupToolProfile = RunInspectionIntention.createProfile(wrapper, managerEx, null);
              managerEx.createNewGlobalContext()
                .codeCleanup(getScope(), cleanupToolProfile, "Cleanup by " + wrapper.getDisplayName(), null, false);
              close(DialogWrapper.OK_EXIT_CODE);
            }
          });
        }
        actions.add(getCancelAction());
        if (SystemInfo.isMac) {
          Collections.reverse(actions);
        }
        return actions.toArray(new Action[0]);
      }
    };

    dialog.showAndGet();
  }

  private static InspectionToolWrapper copyToolWithSettings(@NotNull final InspectionToolWrapper tool) {
    final Element options = new Element("copy");
    tool.getTool().writeSettings(options);
    final InspectionToolWrapper copiedTool = tool.createCopy();
    copiedTool.getTool().readSettings(options);
    return copiedTool;
  }
}
