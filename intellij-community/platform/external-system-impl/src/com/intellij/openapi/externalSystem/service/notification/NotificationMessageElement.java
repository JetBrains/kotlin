// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.icons.AllIcons;
import com.intellij.ide.errorTreeView.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.ui.CustomizeColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import com.intellij.util.ui.tree.WideSelectionTreeUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

import static com.intellij.util.ui.EmptyIcon.ICON_16;

/**
 * @author Vladislav.Soroka
 */
public class NotificationMessageElement extends NavigatableMessageElement {
  public static final String MSG_STYLE = "messageStyle";
  public static final String LINK_STYLE = "linkStyle";

  @NotNull private final CustomizeColoredTreeCellRenderer myLeftTreeCellRenderer;
  @NotNull private final CustomizeColoredTreeCellRenderer myRightTreeCellRenderer;

  public NotificationMessageElement(@NotNull final ErrorTreeElementKind kind,
                                    @Nullable GroupingElement parent,
                                    String[] message,
                                    @NotNull Navigatable navigatable,
                                    String exportText,
                                    String rendererTextPrefix) {
    super(kind, parent, message, navigatable, exportText, rendererTextPrefix);
    myLeftTreeCellRenderer = new CustomizeColoredTreeCellRenderer() {
      @Override
      public void customizeCellRenderer(SimpleColoredComponent renderer,
                                        JTree tree,
                                        Object value,
                                        boolean selected,
                                        boolean expanded,
                                        boolean leaf,
                                        int row,
                                        boolean hasFocus) {
        renderer.setIcon(getIcon(kind));
        renderer.setFont(tree.getFont());
        renderer.append(NewErrorTreeRenderer.calcPrefix(NotificationMessageElement.this));
      }

      @NotNull
      private Icon getIcon(@NotNull ErrorTreeElementKind kind) {
        Icon icon = ICON_16;
        switch (kind) {
          case INFO:
            icon = AllIcons.General.Information;
            break;
          case ERROR:
            icon = AllIcons.General.Error;
            break;
          case WARNING:
            icon = AllIcons.General.Warning;
            break;
          case NOTE:
            icon = AllIcons.General.Tip;
            break;
          case GENERIC:
            icon = ICON_16;
            break;
        }
        return icon;
      }
    };

    myRightTreeCellRenderer = new MyCustomizeColoredTreeCellRendererReplacement();
  }

  @Nullable
  @Override
  public CustomizeColoredTreeCellRenderer getRightSelfRenderer() {
    return myRightTreeCellRenderer;
  }

  @Nullable
  @Override
  public CustomizeColoredTreeCellRenderer getLeftSelfRenderer() {
    return myLeftTreeCellRenderer;
  }

  protected JEditorPane installJep(@NotNull JEditorPane myEditorPane) {
    String message = StringUtil.join(this.getText(), "<br>");
    myEditorPane.setEditable(false);
    myEditorPane.setOpaque(false);
    myEditorPane.setEditorKit(UIUtil.getHTMLEditorKit());
    myEditorPane.setHighlighter(null);

    final StyleSheet styleSheet = ((HTMLDocument)myEditorPane.getDocument()).getStyleSheet();
    final Style style = styleSheet.addStyle(MSG_STYLE, null);
    styleSheet.addStyle(LINK_STYLE, style);
    myEditorPane.setText(message);

    return myEditorPane;
  }

  protected void updateStyle(@NotNull JEditorPane editorPane, @Nullable JTree tree, Object value, boolean selected, boolean hasFocus) {
    final HTMLDocument htmlDocument = (HTMLDocument)editorPane.getDocument();
    final Style style = htmlDocument.getStyleSheet().getStyle(MSG_STYLE);
    if (value instanceof LoadingNode) {
      StyleConstants.setForeground(style, JBColor.GRAY);
    }
    else {
      StyleConstants.setForeground(style, UIUtil.getTreeForeground(selected, hasFocus));
    }

    if (tree != null && WideSelectionTreeUI.isWideSelection(tree)) {
      editorPane.setOpaque(false);
    }
    else {
      editorPane.setOpaque(selected && hasFocus);
    }

    htmlDocument.setCharacterAttributes(0, htmlDocument.getLength(), style, false);
  }

  private class MyCustomizeColoredTreeCellRendererReplacement extends CustomizeColoredTreeCellRendererReplacement {
    @NotNull
    private final JEditorPane myEditorPane;

    private MyCustomizeColoredTreeCellRendererReplacement() {
      myEditorPane = installJep(new MyEditorPane());
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      updateStyle(myEditorPane, tree, value, selected, hasFocus);
      return myEditorPane;
    }

    /**
     * Specialization of {@link JEditorPane} that exposes a simple label
     * as its accessibility model. This is required because exposing
     * a full text editor accessibility model for an error message
     * that eventually ends up in a tree view node makes the user
     * experience confusing for visually impaired users.
     */
    private class MyEditorPane extends JEditorPane {
      @Override
      public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
          return new AccessibleMyEditorPane();
        }
        return accessibleContext;
      }

      protected class AccessibleMyEditorPane extends AccessibleJComponent {
        @Override
        public AccessibleRole getAccessibleRole() {
          return AccessibleRole.LABEL;
        }

        @Override
        public String getAccessibleName() {
          try {
            Document document = MyEditorPane.this.getDocument();
            String result = document.getText(0, document.getLength());
            return AccessibleContextUtil.replaceLineSeparatorsWithPunctuation(result);
          }
          catch (BadLocationException e) {
            return super.getAccessibleName();
          }
        }
      }
    }
  }
}
