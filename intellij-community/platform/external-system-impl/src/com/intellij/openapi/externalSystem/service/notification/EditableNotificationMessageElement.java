// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.ide.IdeTooltipManager;
import com.intellij.ide.errorTreeView.EditableMessageElement;
import com.intellij.ide.errorTreeView.ErrorTreeElementKind;
import com.intellij.ide.errorTreeView.GroupingElement;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.actionSystem.*;
import com.intellij.pom.Navigatable;
import com.intellij.ui.PopupHandler;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class EditableNotificationMessageElement extends NotificationMessageElement implements EditableMessageElement {

  @NotNull private final TreeCellEditor myRightTreeCellEditor;
  @NotNull private final Notification myNotification;
  @NotNull private final Map<String/*url*/, String/*link text to replace*/> disabledLinks;

  public EditableNotificationMessageElement(@NotNull Notification notification,
                                            @NotNull ErrorTreeElementKind kind,
                                            @Nullable GroupingElement parent,
                                            String[] message,
                                            @NotNull Navigatable navigatable,
                                            String exportText, String rendererTextPrefix) {
    super(kind, parent, message, navigatable, exportText, rendererTextPrefix);
    myNotification = notification;
    disabledLinks = new HashMap<>();
    myRightTreeCellEditor = new MyCellEditor();
  }


  public void addDisabledLink(@NotNull String url, @Nullable String text) {
    disabledLinks.put(url, text);
  }

  @NotNull
  @Override
  public TreeCellEditor getRightSelfEditor() {
    return myRightTreeCellEditor;
  }

  @Override
  public boolean startEditingOnMouseMove() {
    return true;
  }

  public static void disableLink(@NotNull HyperlinkEvent event) {
    disableLink(event, null);
  }

  private static void disableLink(@NotNull final HyperlinkEvent event, @Nullable final String linkText) {
    if (event.getSource() instanceof MyJEditorPane) {
      UIUtil.invokeLaterIfNeeded(() -> {
        final MyJEditorPane editorPane = (MyJEditorPane)event.getSource();
        editorPane.myElement.addDisabledLink(event.getDescription(), linkText);
        editorPane.myElement.updateStyle(editorPane, null, null, true, false);
      });
    }
  }

  @Override
  protected void updateStyle(@NotNull JEditorPane editorPane, @Nullable JTree tree, Object value, boolean selected, boolean hasFocus) {
    super.updateStyle(editorPane, tree, value, selected, hasFocus);

    final HTMLDocument htmlDocument = (HTMLDocument)editorPane.getDocument();
    final Style linkStyle = htmlDocument.getStyleSheet().getStyle(LINK_STYLE);
    StyleConstants.setForeground(linkStyle, IdeTooltipManager.getInstance().getLinkForeground(false));
    StyleConstants.setItalic(linkStyle, true);
    HTMLDocument.Iterator iterator = htmlDocument.getIterator(HTML.Tag.A);
    while (iterator.isValid()) {
      boolean disabledLink = false;
      final AttributeSet attributes = iterator.getAttributes();
      if (attributes instanceof SimpleAttributeSet) {
        final Object attribute = attributes.getAttribute(HTML.Attribute.HREF);
        if (attribute instanceof String && disabledLinks.containsKey(attribute)) {
          disabledLink = true;
          //TODO [Vlad] add support for disabled link text update
          ////final String linkText = disabledLinks.get(attribute);
          //if (linkText != null) {
          //}
          ((SimpleAttributeSet)attributes).removeAttribute(HTML.Attribute.HREF);
        }
        if (attribute == null) {
          disabledLink = true;
        }
      }
      if (!disabledLink) {
        htmlDocument.setCharacterAttributes(
          iterator.getStartOffset(), iterator.getEndOffset() - iterator.getStartOffset(), linkStyle, false);
      }
      iterator.next();
    }
  }

  private static class MyJEditorPane extends JEditorPane {
    @NotNull
    private final EditableNotificationMessageElement myElement;

    MyJEditorPane(@NotNull EditableNotificationMessageElement element) {
      myElement = element;
    }
  }

  private class MyCellEditor extends AbstractCellEditor implements TreeCellEditor {
    private final JEditorPane editorComponent;
    @Nullable
    private JTree myTree;

    private MyCellEditor() {
      editorComponent = installJep(new MyJEditorPane(EditableNotificationMessageElement.this));

      HyperlinkListener hyperlinkListener = new ActivatedHyperlinkListener();
      editorComponent.addHyperlinkListener(hyperlinkListener);
      editorComponent.addMouseListener(new PopupHandler() {
        @Override
        public void invokePopup(Component comp, int x, int y) {
          if (myTree == null) return;

          final TreePath path = myTree.getLeadSelectionPath();
          if (path == null) {
            return;
          }
          DefaultActionGroup group = new DefaultActionGroup();
          group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
          group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));

          ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.COMPILER_MESSAGES_POPUP, group);
          menu.getComponent().show(comp, x, y);
        }
      });
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
      myTree = tree;
      updateStyle(editorComponent, tree, value, selected, false);
      return editorComponent;
    }

    @Override
    public Object getCellEditorValue() {
      return null;
    }

    private class ActivatedHyperlinkListener implements HyperlinkListener {

      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          final NotificationListener notificationListener = myNotification.getListener();
          if (notificationListener != null) {
            notificationListener.hyperlinkUpdate(myNotification, e);
          }
        }
      }
    }
  }
}
