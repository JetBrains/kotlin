// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.debugger.extensions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.debugger.UiDebuggerExtension;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;

import static com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

public class ActionTracer implements UiDebuggerExtension, AnActionListener {
  private static final Logger LOG = Logger.getInstance("ActionTracer");

  private JTextArea myText;
  private JPanel myComponent;
  private Disposable myListenerDisposable;

  @Override
  public JComponent getComponent() {
    if (myComponent == null) {
      myText = new JTextArea();
      final JBScrollPane log = new JBScrollPane(myText);
      final AnAction clear = new AnAction("Clear", "Clear log", AllIcons.General.Reset) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          myText.setText(null);
        }
      };
      myComponent = new JPanel(new BorderLayout());
      final DefaultActionGroup group = new DefaultActionGroup();
      group.add(clear);
      myComponent.add(ActionManager.getInstance().createActionToolbar("ActionTracer", group, true).getComponent(), BorderLayout.NORTH);
      myComponent.add(log);

      myListenerDisposable = Disposer.newDisposable();
      ApplicationManager.getApplication().getMessageBus().connect(myListenerDisposable).subscribe(AnActionListener.TOPIC, this);
    }

    return myComponent;
  }

  @Override
  public String getName() {
    return "Actions";
  }

  @Override
  public void afterActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
    StringBuilder out = new StringBuilder(String.format("%1$tF %1$tT,%1$tL ", System.currentTimeMillis()));
    final ActionManager actionManager = ActionManager.getInstance();
    final String id = actionManager.getId(action);
    out.append("id=").append(id);
    if (id != null) {
      out.append("; shortcuts:");
      final Shortcut[] shortcuts = getActiveKeymapShortcuts(id).getShortcuts();
      for (int i = 0; i < shortcuts.length; i++) {
        Shortcut shortcut = shortcuts[i];
        out.append(shortcut);
        if (i < shortcuts.length - 1) {
          out.append(",");
        }
      }
    }
    out.append("; class: ").append(action.getClass().getName());
    out.append("\n");
    final Document doc = myText.getDocument();
    try {
      doc.insertString(doc.getLength(), out.toString(), null);
      SwingUtilities.invokeLater(() -> {
        final int y = (int)myText.getBounds().getMaxY();
        myText.scrollRectToVisible(new Rectangle(0, y, myText.getBounds().width, 0));
      });
    }
    catch (BadLocationException e) {
      LOG.error(e);
    }
  }

  @Override
  public void disposeUiResources() {
    Disposable disposable = myListenerDisposable;
    if (disposable != null) {
      myListenerDisposable = null;
      Disposer.dispose(disposable);
    }
    myComponent = null;
    myText = null;
  }
}
