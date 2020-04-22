/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.find.impl;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.FindUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.util.MinimizeButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ResourceUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;

/**
 * @author spleaner
 */
public class RegExHelpPopup extends JPanel {
  private static final Logger LOG = Logger.getInstance(RegExHelpPopup.class);
  private final JEditorPane myEditorPane;
  private final JScrollPane myScrollPane;

  public RegExHelpPopup() {
    setLayout(new BorderLayout());

    myEditorPane = new JEditorPane();
    myEditorPane.setEditable(false);
    myEditorPane.setEditorKit(UIUtil.getHTMLEditorKit());
    myEditorPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myEditorPane.setBackground(HintUtil.getInformationColor());

    String text;
    try {
      text = ResourceUtil.loadText(ResourceUtil.getResourceAsStream(getClass(), "messages", "RegExHelpPopup.html"));
    }
    catch (IOException e) {
      LOG.error(e);
      text = "Failed to load help page: " + e.getMessage();
    }
    myEditorPane.setText(StringUtil.replace(text, "LABEL_BACKGROUND", ColorUtil.toHtmlColor(UIUtil.getLabelBackground())));

    myEditorPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) BrowserUtil.browse(e.getURL());
      }
    });

    myEditorPane.setCaretPosition(0);

    myScrollPane = ScrollPaneFactory.createScrollPane(myEditorPane);
    myScrollPane.setBorder(null);

    add(myScrollPane, BorderLayout.CENTER);
  }

  public static LinkLabel createRegExLink(@NotNull String title, @Nullable Component owner, @Nullable Logger logger) {
    return createRegExLink(title, owner, logger, null);
  }

  @NotNull
  public static LinkLabel createRegExLink(@NotNull String title, @Nullable Component owner, @Nullable Logger logger, @Nullable String place) {
    Runnable action = createRegExLinkRunnable(owner);
    return new LinkLabel<>(title, null, new LinkListener<Object>() {

      @Override
      public void linkSelected(LinkLabel<Object> aSource, Object aLinkData) {
        FindUtil.triggerRegexHelpClicked(place);
        action.run();
      }
    });
  }

  @NotNull
  public static Runnable createRegExLinkRunnable(@Nullable Component owner) {
    return new Runnable() {
      JBPopup helpPopup;

      @Override
      public void run() {
        if (helpPopup != null && !helpPopup.isDisposed() && helpPopup.isVisible()) {
          return;
        }
        RegExHelpPopup content = new RegExHelpPopup();
        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(content, content);
        helpPopup = builder
          .setCancelOnClickOutside(false)
          .setBelongsToGlobalPopupStack(true)
          .setFocusable(true)
          .setRequestFocus(true)
          .setMovable(true)
          .setResizable(true)
          .setCancelOnOtherWindowOpen(false).setCancelButton(new MinimizeButton("Hide"))
          .setTitle(LangBundle.message("popup.title.regular.expressions.syntax")).setDimensionServiceKey(null, "RegExHelpPopup", true).createPopup();
        Disposer.register(helpPopup, new Disposable() {
          @Override
          public void dispose() {
            destroyPopup();
          }
        });
        if (owner != null) {
          helpPopup.showInCenterOf(owner);
        }
        else {
          helpPopup.showInFocusCenter();
        }
      }

      private void destroyPopup() {
        helpPopup = null;
      }
    };
  }

  @Override
  public Dimension getPreferredSize() {
    return JBUI.size(600, 300);
  }
}
