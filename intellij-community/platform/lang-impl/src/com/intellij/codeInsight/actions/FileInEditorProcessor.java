/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.actions;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightweightHint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

import static com.intellij.codeInsight.actions.TextRangeType.SELECTED_TEXT;
import static com.intellij.codeInsight.actions.TextRangeType.VCS_CHANGED_TEXT;

public class FileInEditorProcessor {
  private static final Logger LOG = Logger.getInstance(FileInEditorProcessor.class);

  private final Editor myEditor;
  private final boolean myShouldCleanupCode;

  private boolean myNoChangesDetected = false;
  private final boolean myProcessChangesTextOnly;

  private final boolean myShouldOptimizeImports;
  private final boolean myShouldRearrangeCode;
  private final boolean myProcessSelectedText;

  private final Project myProject;

  private final PsiFile myFile;
  private AbstractLayoutCodeProcessor myProcessor;

  public FileInEditorProcessor(PsiFile file,
                               Editor editor,
                               LayoutCodeOptions runOptions)
  {
    myFile = file;
    myProject = file.getProject();
    myEditor = editor;

    myShouldCleanupCode = runOptions.isCodeCleanup();
    myShouldOptimizeImports = runOptions.isOptimizeImports();
    myShouldRearrangeCode = runOptions.isRearrangeCode();
    myProcessSelectedText = myEditor != null && runOptions.getTextRangeType() == SELECTED_TEXT;
    myProcessChangesTextOnly = runOptions.getTextRangeType() == VCS_CHANGED_TEXT;
  }

  public void processCode() {
    if (!CodeStyle.isFormattingEnabled(myFile)) {
      if (!isInHeadlessMode() && !myEditor.isDisposed() && myEditor.getComponent().isShowing()) {
        showHint(myEditor, new DisabledFormattingMessageBuilder());
      }
      return;
    }

    if (myShouldOptimizeImports) {
      myProcessor = new OptimizeImportsProcessor(myProject, myFile);
    }

    if (myProcessChangesTextOnly && !FormatChangedTextUtil.hasChanges(myFile)) {
      myNoChangesDetected = true;
    }

    myProcessor = mixWithReformatProcessor(myProcessor);
    if (myShouldRearrangeCode) {
      myProcessor = mixWithRearrangeProcessor(myProcessor);
    }

    if (myShouldCleanupCode) {
      myProcessor = mixWithCleanupProcessor(myProcessor);
    }

    if (shouldNotify()) {
      myProcessor.setCollectInfo(true);
      myProcessor.setPostRunnable(() -> {
        if (!myEditor.isDisposed() && myEditor.getComponent().isShowing()) {
          showHint(myEditor, new FormattedMessageBuilder());
        }
      });
    }

    myProcessor.run();
  }

  @NotNull
  private AbstractLayoutCodeProcessor mixWithCleanupProcessor(@NotNull AbstractLayoutCodeProcessor processor) {
    if (myProcessSelectedText) {
      processor = new CodeCleanupCodeProcessor(processor, myEditor.getSelectionModel());
    }
    else {
      processor = new CodeCleanupCodeProcessor(processor);
    }
    return processor;
  }

  private AbstractLayoutCodeProcessor mixWithRearrangeProcessor(@NotNull AbstractLayoutCodeProcessor processor) {
    if (myProcessSelectedText) {
      processor = new RearrangeCodeProcessor(processor, myEditor.getSelectionModel());
    }
    else {
      processor = new RearrangeCodeProcessor(processor);
    }
    return processor;
  }

  @NotNull
  private AbstractLayoutCodeProcessor mixWithReformatProcessor(@Nullable AbstractLayoutCodeProcessor processor) {
    if (processor != null) {
      if (myProcessSelectedText) {
        processor = new ReformatCodeProcessor(processor, myEditor.getSelectionModel());
      }
      else {
        processor = new ReformatCodeProcessor(processor, myProcessChangesTextOnly);
      }
    }
    else {
      if (myProcessSelectedText) {
        processor = new ReformatCodeProcessor(myFile, myEditor.getSelectionModel());
      }
      else {
        processor = new ReformatCodeProcessor(myFile, myProcessChangesTextOnly);
      }
    }
    return processor;
  }

  @NotNull
  private static String joinWithCommaAndCapitalize(String reformatNotification, String rearrangeNotification) {
    String firstNotificationLine = reformatNotification != null ? reformatNotification : rearrangeNotification;
    if (reformatNotification != null && rearrangeNotification != null) {
      firstNotificationLine += ", " + rearrangeNotification;
    }
    firstNotificationLine = StringUtil.capitalize(firstNotificationLine);
    return firstNotificationLine;
  }

  private static void showHint(@NotNull Editor editor, @NotNull MessageBuilder messageBuilder) {
    showHint(editor, messageBuilder.getMessage(), messageBuilder.createHyperlinkListener());
  }

  public static void showHint(@NotNull Editor editor, @NotNull String info, @Nullable HyperlinkListener hyperlinkListener) {
    JComponent component = HintUtil.createInformationLabel(info, hyperlinkListener, null, null);
    LightweightHint hint = new LightweightHint(component);

    int flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING;
    if (EditorUtil.isPrimaryCaretVisible(editor)) {
      HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, HintManager.UNDER, flags, 0, false);
    }
    else {
      showHintWithoutScroll(editor, hint, flags);
    }
  }

  private static void showHintWithoutScroll(Editor editor, LightweightHint hint, int flags) {
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    
    short constraint;
    int y;
    
    if (isCaretAboveTop(editor, visibleArea)) {
      y = visibleArea.y;
      constraint = HintManager.UNDER;
    }
    else {
      y = visibleArea.y + visibleArea.height;
      constraint = HintManager.ABOVE;
    }
    
    Point hintPoint = new Point(visibleArea.x + (visibleArea.width / 2), y);
    
    JComponent component = HintManagerImpl.getExternalComponent(editor);
    Point convertedPoint = SwingUtilities.convertPoint(editor.getContentComponent(), hintPoint, component);
    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, convertedPoint, flags, 0, false, constraint);
  }

  private static boolean isCaretAboveTop(Editor editor, Rectangle area) {
    Caret caret = editor.getCaretModel().getCurrentCaret();
    VisualPosition caretVisualPosition = caret.getVisualPosition();
    int caretY = editor.visualPositionToXY(caretVisualPosition).y;
    return caretY < area.y;
  }

  private boolean shouldNotify() {
    if (isInHeadlessMode()) return false;
    EditorSettingsExternalizable es = EditorSettingsExternalizable.getInstance();
    return es.isShowNotificationAfterReformat() && myEditor != null && !myProcessSelectedText;
  }

  private static boolean isInHeadlessMode() {
    Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode() || application.isHeadlessEnvironment()) {
      return true;
    }
    return false;
  }

  private class DisabledFormattingMessageBuilder extends MessageBuilder {
    @NotNull
    @Override
    public String getMessage() {
      VirtualFile virtualFile = myFile.getVirtualFile();
      String name = virtualFile != null ? virtualFile.getName() : "the file";
      return "<html>" +
             "Formatting is disabled for " + name +
             "<p><span><a href=''>Show settings...</a></span>" +
             "</html>";
    }

    @Override
    public Runnable getHyperlinkRunnable() {
      return () -> ShowSettingsUtilImpl.showSettingsDialog(myProject, "preferences.sourceCode", "Do not format");
    }
  }

  private class FormattedMessageBuilder extends MessageBuilder {
    @Override
    @NotNull
    public String getMessage() {
      StringBuilder builder = new StringBuilder("<html>");
      LayoutCodeInfoCollector notifications = myProcessor.getInfoCollector();
      LOG.assertTrue(notifications != null);

      if (notifications.isEmpty() && !myNoChangesDetected) {
        if (myProcessChangesTextOnly) {
          builder.append("No lines changed: changes since last revision are already properly formatted").append("<br>");
        }
        else {
          builder.append("No lines changed: content is already properly formatted").append("<br>");
        }
      }
      else {
        if (notifications.hasReformatOrRearrangeNotification()) {
          String reformatInfo = notifications.getReformatCodeNotification();
          String rearrangeInfo = notifications.getRearrangeCodeNotification();

          builder.append(joinWithCommaAndCapitalize(reformatInfo, rearrangeInfo));

          if (myProcessChangesTextOnly) {
            builder.append(" in changes since last revision");
          }

          builder.append("<br>");
        }
        else if (myNoChangesDetected) {
          builder.append("No lines changed: no changes since last revision").append("<br>");
        }

        String optimizeImportsNotification = notifications.getOptimizeImportsNotification();
        if (optimizeImportsNotification != null) {
          builder.append(StringUtil.capitalize(optimizeImportsNotification)).append("<br>");
        }
      }

      String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction("ShowReformatFileDialog"));
      String color = ColorUtil.toHex(JBColor.gray);

      builder.append("<span style='color:#").append(color).append("'>")
        .append("<a href=''>Show</a> reformat dialog: ").append(shortcutText).append("</span>")
        .append("</html>");

      return builder.toString();
    }

    @Override
    public Runnable getHyperlinkRunnable() {
      return () -> {
        AnAction action = ActionManager.getInstance().getAction("ShowReformatFileDialog");
        DataManager manager = DataManager.getInstance();
        if (manager != null) {
          DataContext context = manager.getDataContext(myEditor.getContentComponent());
          action.actionPerformed(AnActionEvent.createFromAnAction(action, null, "", context));
        }
      };
    }
  }

  private abstract static class MessageBuilder {
    public abstract String getMessage();

    public abstract Runnable getHyperlinkRunnable();

    public final HyperlinkListener createHyperlinkListener() {
      return new HyperlinkAdapter() {
        @Override
        protected void hyperlinkActivated(HyperlinkEvent e) {
          getHyperlinkRunnable().run();
        }
      };
    }
  }
}
