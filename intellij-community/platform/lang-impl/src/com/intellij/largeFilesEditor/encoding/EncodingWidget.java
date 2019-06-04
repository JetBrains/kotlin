// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.encoding;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class EncodingWidget extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {
  public static final String WIDGET_ID = "lfeEncoding";

  private static final Logger logger = Logger.getInstance(EncodingWidget.class);

  private final TextPanel myComponent;
  private final Alarm myUpdateAlarm;

  private final EditorManagerAccessor editorManagerAccessor;

  private boolean myActionEnabled;

  public EncodingWidget(@NotNull final Project project, EditorManagerAccessor editorManagerAccessor) {
    super(project);

    this.editorManagerAccessor = editorManagerAccessor;

    myUpdateAlarm = new Alarm(this);

    myComponent = new TextPanel.ExtraSize() {
      @Override
      protected void paintComponent(@NotNull final Graphics g) {
        super.paintComponent(g);
        if (myActionEnabled && getText() != null) {
          final Rectangle r = getBounds();
          final Insets insets = getInsets();
          Icon arrows = AllIcons.Ide.Statusbar_arrows;
          arrows.paintIcon(this, g, r.width - insets.right - arrows.getIconWidth() - 2,
                           r.height / 2 - arrows.getIconHeight() / 2);
        }
      }
    };

    myComponent.setBorder(WidgetBorder.WIDE);

    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        requestUpdate();
        tryShowPopup();
        return true;
      }
    }.installOn(myComponent);
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    requestUpdate();
  }

  @Override
  public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    requestUpdate();
  }

  @Override
  public StatusBarWidget copy() {
    if (getProject() != null) {
      return new EncodingWidget(getProject(), editorManagerAccessor);
    }
    else {
      logger.warn("[LargeFileEditorSubsystem] EncodingWidget.copy(): getProject() is Null");
      return null;
    }
  }

  @Override
  @NotNull
  public String ID() {
    return WIDGET_ID;
  }

  @Override
  public WidgetPresentation getPresentation(@NotNull PlatformType type) {
    return null;
  }

  @Override
  public void install(@NotNull StatusBar statusBar) {
    super.install(statusBar);
  }

  private void tryShowPopup() {
    if (!myActionEnabled) {
      return;
    }
    //EditorManager editorManager = tryGetActiveEditorManager();
    EditorManagerAccess editorManagerAccess = editorManagerAccessor.getAccess(myProject, myStatusBar);
    if (editorManagerAccess != null) {
      showPopup(editorManagerAccess);
    }
    else {
      logger.warn("[LargeFileEditorSubsystem] EncodingWidget.tryShowPopup():" +
                  " this method was called while editorManager is not available as active text editor");
      requestUpdate();
    }
  }

  private void showPopup(@NotNull EditorManagerAccess editorManagerAccess) {
    ChangeFileEncodingAction action = new ChangeFileEncodingAction(
      editorManagerAccessor, myProject, myStatusBar);
    JComponent where = getComponent();
    ListPopup popup = action.createPopup(editorManagerAccess.getVirtualFile(), editorManagerAccess.getEditor(),
                                         where);
    RelativePoint pos = JBPopupFactory.getInstance().guessBestPopupLocation(where);
    popup.showInScreenCoordinates(where, pos.getScreenPoint());
  }

  public Project _getProject() {
    return getProject();
  }

  public void requestUpdate() {
    if (myUpdateAlarm.isDisposed()) return;

    myUpdateAlarm.cancelAllRequests();
    myUpdateAlarm.addRequest(() -> update(), 200, ModalityState.any());
  }

  private void update() {
    if (isDisposed()) return;

    EditorManagerAccess editorManagerAccess = editorManagerAccessor.getAccess(myProject, myStatusBar);

    myActionEnabled = false;
    String charsetName;
    String toolTipText;

    if (editorManagerAccess == null) {
      toolTipText = "";
      charsetName = "";
      myComponent.setVisible(false);
    }
    else {
      myActionEnabled = true;
      charsetName = editorManagerAccess.getCharsetName();
      toolTipText = "File Encoding: " + charsetName;
      myComponent.setVisible(true);
    }

    myComponent.setToolTipText(toolTipText);
    myComponent.setText(charsetName);

    if (myStatusBar != null) {
      myStatusBar.updateWidget(ID());
    }
    else {
      logger.warn("[LargeFileEditorSubsystem] EncodingWidget.requestUpdate(): myStatusBar is null!!!)");
    }
  }

  @Override
  public JComponent getComponent() {
    return myComponent;
  }
}
