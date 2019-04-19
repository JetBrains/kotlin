// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.slicer;

import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.RefactoringDescriptionLocation;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@State(name = "SliceManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class SliceManager implements PersistentStateComponent<SliceManager.StoredSettingsBean> {
  private final Project myProject;
  private ContentManager myBackContentManager;
  private ContentManager myForthContentManager;
  private final StoredSettingsBean myStoredSettings = new StoredSettingsBean();
  private static final String BACK_TOOLWINDOW_ID = "Analyze Dataflow to";
  private static final String FORTH_TOOLWINDOW_ID = "Analyze Dataflow from";

  static class StoredSettingsBean {
    boolean showDereferences = true; // to show in dataflow/from dialog
    AnalysisUIOptions analysisUIOptions = new AnalysisUIOptions();
  }

  public static SliceManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, SliceManager.class);
  }

  public SliceManager(@NotNull Project project) {
    myProject = project;
  }

  private ContentManager getContentManager(boolean dataFlowToThis) {
    if (dataFlowToThis) {
      if (myBackContentManager == null) {
        ToolWindow backToolWindow = ToolWindowManager.getInstance(myProject).registerToolWindow(BACK_TOOLWINDOW_ID, true, ToolWindowAnchor.BOTTOM, myProject);
        myBackContentManager = backToolWindow.getContentManager();
        new ContentManagerWatcher(backToolWindow, myBackContentManager);
      }
      return myBackContentManager;
    }

    if (myForthContentManager == null) {
      ToolWindow forthToolWindow = ToolWindowManager.getInstance(myProject).registerToolWindow(FORTH_TOOLWINDOW_ID, true, ToolWindowAnchor.BOTTOM, myProject);
      myForthContentManager = forthToolWindow.getContentManager();
      new ContentManagerWatcher(forthToolWindow, myForthContentManager);
    }
    return myForthContentManager;
  }

  public void slice(@NotNull PsiElement element, boolean dataFlowToThis, @NotNull SliceHandler handler) {
    String dialogTitle = getElementDescription((dataFlowToThis ? BACK_TOOLWINDOW_ID : FORTH_TOOLWINDOW_ID) + " ", element, null);

    dialogTitle = Pattern.compile("(<style>.*</style>)|<[^<>]*>", Pattern.DOTALL).matcher(dialogTitle).replaceAll("");
    SliceAnalysisParams params = handler.askForParams(element, dataFlowToThis, myStoredSettings, StringUtil.unescapeXmlEntities(dialogTitle));
    if (params == null) return;

    SliceRootNode rootNode = new SliceRootNode(myProject, new DuplicateMap(),
                                               LanguageSlicing.getProvider(element).createRootUsage(element, params));

    createToolWindow(dataFlowToThis, rootNode, false, getElementDescription(null, element, null));
  }

  public void createToolWindow(boolean dataFlowToThis, @NotNull SliceRootNode rootNode, boolean splitByLeafExpressions, @NotNull String displayName) {
    final SliceToolwindowSettings sliceToolwindowSettings = SliceToolwindowSettings.getInstance(myProject);
    final ContentManager contentManager = getContentManager(dataFlowToThis);
    final Content[] myContent = new Content[1];
    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(dataFlowToThis ? BACK_TOOLWINDOW_ID : FORTH_TOOLWINDOW_ID);
    final SlicePanel slicePanel = new SlicePanel(myProject, dataFlowToThis, rootNode, splitByLeafExpressions, toolWindow) {
      @Override
      protected void close() {
        super.close();
        contentManager.removeContent(myContent[0], true);
      }

      @Override
      public boolean isAutoScroll() {
        return sliceToolwindowSettings.isAutoScroll();
      }

      @Override
      public void setAutoScroll(boolean autoScroll) {
        sliceToolwindowSettings.setAutoScroll(autoScroll);
      }

      @Override
      public boolean isPreview() {
        return sliceToolwindowSettings.isPreview();
      }

      @Override
      public void setPreview(boolean preview) {
        sliceToolwindowSettings.setPreview(preview);
      }
    };

    myContent[0] = contentManager.getFactory().createContent(slicePanel, displayName, true);
    contentManager.addContent(myContent[0]);
    contentManager.setSelectedContent(myContent[0]);

    toolWindow.activate(null);
  }

  public static String getElementDescription(String prefix, PsiElement element, String suffix) {
    SliceLanguageSupportProvider provider = LanguageSlicing.getProvider(element);
    if(provider != null){
      element = provider.getElementForDescription(element);
    }
    String desc = ElementDescriptionUtil.getElementDescription(element, RefactoringDescriptionLocation.WITHOUT_PARENT);
    return "<html><body>" +
           (prefix == null ? "" : prefix) + StringUtil.first(desc, 100, true)+(suffix == null ? "" : suffix) +
           "</body></html>";
  }

  @Override
  public StoredSettingsBean getState() {
    return myStoredSettings;
  }

  @Override
  public void loadState(@NotNull StoredSettingsBean state) {
    myStoredSettings.analysisUIOptions.loadState(state.analysisUIOptions);
  }
}
