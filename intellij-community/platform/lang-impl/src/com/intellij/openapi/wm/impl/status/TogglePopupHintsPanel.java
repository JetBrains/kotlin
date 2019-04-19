/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.openapi.wm.impl.status;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.HectorComponent;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
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

import static com.intellij.openapi.util.IconLoader.getDisabledIcon;

public class TogglePopupHintsPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
  private Icon myCurrentIcon;
  private String myToolTipText;

  public TogglePopupHintsPanel(@NotNull final Project project) {
    super(project);
    myCurrentIcon = getDisabledIcon(AllIcons.Ide.HectorOff);
    myConnection.subscribe(PowerSaveMode.TOPIC, this::updateStatus);
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
  @NotNull
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
      Point point = new Point(0, 0);
      final PsiFile file = getCurrentFile();
      if (file != null) {
        if (!DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file)) return;
        final HectorComponent component = new HectorComponent(file);
        final Dimension dimension = component.getPreferredSize();
        point = new Point(point.x - dimension.width, point.y - dimension.height);
        component.showComponent(new RelativePoint(e.getComponent(), point));
      }
    };
  }

  @Override
  @NotNull
  public String ID() {
    return "InspectionProfile";
  }

  @Override
  public WidgetPresentation getPresentation(@NotNull PlatformType type) {
    return this;
  }

  public void clear() {
    myCurrentIcon = getDisabledIcon(AllIcons.Ide.HectorOff);
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
        myCurrentIcon = getDisabledIcon(AllIcons.Ide.HectorOff);
        myToolTipText = "Code analysis is disabled in power save mode.\n";
      }
      else if (HighlightingLevelManager.getInstance(myProject).shouldInspect(file)) {
        myCurrentIcon = AllIcons.Ide.HectorOn;
        myToolTipText = "Current inspection profile: " +
                        InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile().getName() +
                        ".\n";
      }
      else if (HighlightingLevelManager.getInstance(myProject).shouldHighlight(file)) {
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
      myCurrentIcon = getDisabledIcon(AllIcons.Ide.HectorOff);
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
