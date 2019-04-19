// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ui.debugger;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.UiDecorator;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class UiDebugger extends JPanel implements Disposable {

  private final DialogWrapper myDialog;
  private final JBTabs myTabs;
  private final List<UiDebuggerExtension> myExtensions;

  public UiDebugger() {
    Disposer.register(Disposer.get("ui"), this);

    myTabs = JBTabsFactory.createTabs(null, this);
    myTabs.getPresentation().setInnerInsets(new Insets(4, 0, 0, 0)).setPaintBorder(1, 0, 0, 0).setActiveTabFillIn(JBColor.GRAY).setUiDecorator(new UiDecorator() {
      @Override
      @NotNull
      public UiDecoration getDecoration() {
        return new UiDecoration(null, JBUI.insets(4));
      }
    });

    myExtensions = UiDebuggerExtension.EP_NAME.getExtensionList();
    addToUi(myExtensions);

    myDialog = new DialogWrapper((Project)null, true) {
      {
        init();
      }

      @Override
      protected JComponent createCenterPanel() {
        Disposer.register(getDisposable(), UiDebugger.this);
        return myTabs.getComponent();
      }

      @Override
      public JComponent getPreferredFocusedComponent() {
        return myTabs.getComponent();
      }

      @Override
      protected String getDimensionServiceKey() {
        return "UiDebugger";
      }

      @Override
      protected JComponent createSouthPanel() {
        final JPanel result = new JPanel(new BorderLayout());
        result.add(super.createSouthPanel(), BorderLayout.EAST);
        final JSlider slider = new JSlider(0, 100);
        slider.setValue(100);
        slider.addChangeListener(new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            final int value = slider.getValue();
            float alpha = value / 100f;

            final Window wnd = SwingUtilities.getWindowAncestor(slider);
            if (wnd != null) {
              final WindowManagerEx mgr = WindowManagerEx.getInstanceEx();
              if (value == 100) {
                mgr.setAlphaModeEnabled(wnd, false);
              } else {
                mgr.setAlphaModeEnabled(wnd, true);
                mgr.setAlphaModeRatio(wnd, 1f - alpha);
              }
            }
          }
        });
        result.add(slider, BorderLayout.WEST);
        return result;
      }

      @NotNull
      @Override
      protected Action[] createActions() {
        return new Action[] {new AbstractAction("Close") {
          @Override
          public void actionPerformed(ActionEvent e) {
            doOKAction();
          }
        }};
      }
    };
    myDialog.setModal(false);
    myDialog.setTitle("UI Debugger");
    myDialog.setResizable(true);

    myDialog.show();
  }

  @Override
  public void show() {
    myDialog.getPeer().getWindow().toFront();
  }

  private void addToUi(List<UiDebuggerExtension> extensions) {
    for (UiDebuggerExtension each : extensions) {
      myTabs.addTab(new TabInfo(each.getComponent()).setText(each.getName()));
    }
  }

  @Override
  public void dispose() {
    for (UiDebuggerExtension each : myExtensions) {
      each.disposeUiResources();
    }
  }
}
