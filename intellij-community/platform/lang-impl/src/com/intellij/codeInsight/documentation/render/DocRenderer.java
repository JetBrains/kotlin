// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsUtil;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

class DocRenderer implements EditorCustomElementRenderer {
  static final Key<Boolean> RECREATE_COMPONENT = Key.create("doc.renderer.recreate.component");

  private static final int MIN_WIDTH = 350;
  private static final int MAX_WIDTH = 680;
  private static final int ARC_WIDTH = 5;
  private static final int LEFT_INSET = 12;
  private static final int RIGHT_INSET = 20;
  private static final int TOP_BOTTOM_INSETS = 8;
  private static final int BOTTOM_MARGIN = 10;

  private final DocRenderItem myItem;
  private JEditorPane myPane;

  DocRenderer(DocRenderItem item) {
    myItem = item;
  }

  @Override
  public int calcWidthInPixels(@NotNull Inlay inlay) {
    return calcInlayWidth(inlay.getEditor());
  }

  @Override
  public int calcHeightInPixels(@NotNull Inlay inlay) {
    Editor editor = inlay.getEditor();
    JComponent component = getRendererComponent(inlay);
    AppUIUtil.targetToDevice(component, editor.getContentComponent());
    component.setSize(Math.max(0, calcInlayWidth(editor) - calcInlayStartX() - scale(LEFT_INSET) - scale(RIGHT_INSET)), Integer.MAX_VALUE);
    return component.getPreferredSize().height + scale(TOP_BOTTOM_INSETS) * 2 + scale(BOTTOM_MARGIN);
  }

  @Override
  public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
    int startX = calcInlayStartX();
    if (startX >= targetRegion.width) return;
    int bottomMargin = scale(BOTTOM_MARGIN);
    if (bottomMargin >= targetRegion.height) return;
    int arcSize = scale(ARC_WIDTH);
    Color bgColor = EditorColorsUtil.getGlobalOrDefaultColor(EditorColors.DOCUMENTATION_COLOR);

    g.setColor(bgColor);
    Object savedAntiAliasing = ((Graphics2D)g).getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.fillRoundRect(startX, targetRegion.y, targetRegion.width - startX, targetRegion.height - bottomMargin, arcSize, arcSize);
    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAntiAliasing);

    JComponent component = getRendererComponent(inlay);
    component.setBackground(bgColor);
    int componentWidth = targetRegion.width - startX - scale(LEFT_INSET) - scale(RIGHT_INSET);
    int componentHeight = targetRegion.height - scale(TOP_BOTTOM_INSETS) * 2 - bottomMargin;
    if (componentWidth > 0 && componentHeight > 0) {
      component.setSize(componentWidth, componentHeight);
      Graphics dg = g.create(startX + scale(LEFT_INSET), targetRegion.y + arcSize, componentWidth, componentHeight);
      GraphicsUtil.setupAntialiasing(dg);
      component.paint(dg);
      dg.dispose();
    }
  }

  @Override
  public GutterIconRenderer calcGutterIconProvider(@NotNull Inlay inlay) {
    return myItem.highlighter.getGutterIconRenderer();
  }

  @Override
  public ActionGroup getContextMenuGroup(@NotNull Inlay inlay) {
    return new DefaultActionGroup(Objects.requireNonNull(myItem.highlighter.getGutterIconRenderer()).getClickAction(),
                                  new DocRenderItem.ChangeFontSize());
  }

  private static int scale(int value) {
    return (int)(value * UISettings.getDefFontScale());
  }

  static int calcInlayWidth(@NotNull Editor editor) {
    return Math.max(scale(MIN_WIDTH), Math.min(scale(MAX_WIDTH), editor.getScrollingModel().getVisibleArea().width));
  }

  private int calcInlayStartX() {
    Document document = myItem.editor.getDocument();
    int lineStartOffset = document.getLineStartOffset(document.getLineNumber(myItem.highlighter.getEndOffset()) + 1);
    int contentStartOffset = CharArrayUtil.shiftForward(document.getImmutableCharSequence(), lineStartOffset, " \t");
    int indentPixels = myItem.editor.offsetToXY(contentStartOffset, false, true).x;
    return Math.max(0, indentPixels - scale(LEFT_INSET));
  }

  private JComponent getRendererComponent(Inlay inlay) {
    if (myPane == null || Boolean.TRUE.equals(inlay.getUserData(RECREATE_COMPONENT) != null)) {
      myPane = new JEditorPane(UIUtil.HTML_MIME, "");
      myPane.setEditable(false);
      myPane.putClientProperty("caretWidth", 0); // do not reserve space for caret (making content one pixel narrower than component)
      myPane.setEditorKit(createEditorKit());
      myPane.setBorder(JBUI.Borders.empty());
      myPane.setFont(myPane.getFont().deriveFont((float)JBUIScale.scale(DocumentationComponent.getQuickDocFontSize().getSize())));
      myPane.setText(myItem.textToRender);
      inlay.putUserData(RECREATE_COMPONENT, null);
    }
    return myPane;
  }

  private static JBHtmlEditorKit createEditorKit() {
    JBHtmlEditorKit editorKit = new JBHtmlEditorKit(true);
    String editorFontName = StringUtil.escapeQuotes(EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName());
    editorKit.getStyleSheet().addRule("code {font-family:\"" + editorFontName + "\"}");
    editorKit.getStyleSheet().addRule("pre {font-family:\"" + editorFontName + "\"}");
    editorKit.getStyleSheet().addRule("h1, h2, h3, h4, h5, h6 { margin-top: 0; padding-top: 1px; }");
    editorKit.getStyleSheet().addRule("a { color: #" + ColorUtil.toHex(JBUI.CurrentTheme.Link.linkColor()) + "; text-decoration: none;}");
    editorKit.getStyleSheet().addRule("p { padding: 1px 0 2px 0; }");
    editorKit.getStyleSheet().addRule("ol { padding: 0 16px 0 0; }");
    editorKit.getStyleSheet().addRule("ul { padding: 0 16px 0 0; }");
    editorKit.getStyleSheet().addRule("li { padding: 1px 0 2px 0; }");
    editorKit.getStyleSheet().addRule("table p { padding-bottom: 0}");
    editorKit.getStyleSheet().addRule("td { margin: 4px 0 0 0; padding: 0; }");
    editorKit.getStyleSheet().addRule("th { text-align: left; }");
    editorKit.getStyleSheet().addRule(".grayed { color: #909090; display: inline;}");
    editorKit.getStyleSheet().addRule(".section { color: " + ColorUtil.toHtmlColor(DocumentationComponent.SECTION_COLOR) +
                                      "; padding-right: 4px}");
    return editorKit;
  }
}
