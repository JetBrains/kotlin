// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.wm.impl.status;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.HectorComponent;
import com.intellij.codeInsight.daemon.impl.HectorComponentFactory;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSettingListener;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.profile.ProfileChangeAdapter;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.UIBundle;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TogglePopupHintsPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
  private Icon myCurrentIcon;
  private String myToolTipText;

  public TogglePopupHintsPanel(@NotNull Project project) {
    super(project);
    myCurrentIcon = IconLoader.getDisabledIcon(AllIcons.Ide.HectorOff);
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    updateStatus();
  }


  @Override
  public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    updateStatus();
  }

  @Override
  public StatusBarWidget copy() {
    return new TogglePopupHintsPanel(getProject());
  }

  @Override
  @Nullable
  public Icon getIcon() {
    return myCurrentIcon;
  }

  @Override
  public String getTooltipText() {
    return myToolTipText;
  }

  @Override
  public Consumer<MouseEvent> getClickConsumer() {
    return e -> {
      final PsiFile file = getCurrentFile();
      if (file != null) {
        if (!DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file)) return;
        final HectorComponent component = ServiceManager.getService(myProject, HectorComponentFactory.class).create(file);
        component.showComponent(e.getComponent(), d -> new Point(-d.width, -d.height));
      }
    };
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
    super.install(statusBar);
    myConnection.subscribe(PowerSaveMode.TOPIC, this::updateStatus);
    myConnection.subscribe(ProfileChangeAdapter.TOPIC,  new ProfileChangeAdapter() {
      @Override
      public void profilesInitialized() {
        updateStatus();
      }
      @Override
      public void profileActivated(InspectionProfile oldProfile, @Nullable InspectionProfile profile) {
        updateStatus();
      }

      @Override
      public void profileChanged(InspectionProfile profile) {
        updateStatus();
      }
    });

    myConnection.subscribe(FileHighlightingSettingListener.SETTING_CHANGE, (__, ___) -> updateStatus());
  }

  @Override
  @NotNull
  public String ID() {
    return "InspectionProfile";
  }

  @Override
  public WidgetPresentation getPresentation() {
    return this;
  }

  public void clear() {
    myCurrentIcon = IconLoader.getDisabledIcon(AllIcons.Ide.HectorOff);
    myToolTipText = null;
    myStatusBar.updateWidget(ID());
  }

  public void updateStatus() {
    UIUtil.invokeLaterIfNeeded(() -> updateStatus(getCurrentFile()));
  }



  private void updateStatus(PsiFile file) {
    if (isDisposed()) return;
    if (isStateChangeable(file)) {
      if (PowerSaveMode.isEnabled()) {
        myCurrentIcon = IconLoader.getDisabledIcon(AllIcons.Ide.HectorOff);
        myToolTipText = "Code analysis is disabled in power save mode.\n";
      }
      else if (HighlightingLevelManager.getInstance(getProject()).shouldInspect(file)) {
        myCurrentIcon = AllIcons.Ide.HectorOn;
        InspectionProfileImpl profile = InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile();
        if (profile.wasInitialized())
          myToolTipText = "Current inspection profile: " +profile .getName() + ".\n";
      }
      else if (HighlightingLevelManager.getInstance(getProject()).shouldHighlight(file)) {
        myCurrentIcon = AllIcons.Ide.HectorSyntax;
        myToolTipText = "Highlighting level is: Syntax.\n";
      }
      else {
        myCurrentIcon = AllIcons.Ide.HectorOff;
        myToolTipText = "Inspections are off.\n";
      }
      myToolTipText += UIBundle.message("popup.hints.panel.click.to.configure.highlighting.tooltip.text");
    }
    else {
      myCurrentIcon = file != null ? IconLoader.getDisabledIcon(AllIcons.Ide.HectorOff) : null;
      myToolTipText = null;
    }

    if (!ApplicationManager.getApplication().isUnitTestMode() && myStatusBar != null) {
      myStatusBar.updateWidget(ID());
    }
  }

  private static boolean isStateChangeable(PsiFile file) {
    return file != null && DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file);
  }

  @Nullable
  private PsiFile getCurrentFile() {
    VirtualFile virtualFile = getSelectedFile();
    if (virtualFile != null && virtualFile.isValid()){
      return PsiManager.getInstance(getProject()).findFile(virtualFile);
    }
    return null;
  }
}
