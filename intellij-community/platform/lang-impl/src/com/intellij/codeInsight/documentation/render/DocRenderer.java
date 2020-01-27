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
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class DocRenderer implements EditorCustomElementRenderer {
  static final Key<Boolean> RECREATE_COMPONENT = Key.create("doc.renderer.recreate.component");

  private static final int MIN_WIDTH = 350;
  private static final int MAX_WIDTH = 680;
  private static final int ARC_WIDTH = 5;
  private static final int LEFT_INSET = 12;
  private static final int RIGHT_INSET = 20;
  private static final int TOP_BOTTOM_INSETS = 8;

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
    return component.getPreferredSize().height + scale(TOP_BOTTOM_INSETS) * 2 + scale(getTopMargin()) + scale(getBottomMargin());
  }

  @Override
  public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
    int startX = calcInlayStartX();
    if (startX >= targetRegion.width) return;
    int topMargin = scale(getTopMargin());
    int bottomMargin = scale(getBottomMargin());
    int filledHeight = targetRegion.height - topMargin - bottomMargin;
    if (filledHeight <= 0) return;
    int filledStartY = targetRegion.y + topMargin;
    int arcSize = scale(ARC_WIDTH);
    Color bgColor = getColorFromRegistry("editor.render.doc.comments.bg");

    g.setColor(bgColor);
    Object savedAntiAliasing = ((Graphics2D)g).getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.fillRoundRect(startX, filledStartY, targetRegion.width - startX, filledHeight, arcSize, arcSize);
    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAntiAliasing);

    JComponent component = getRendererComponent(inlay);
    component.setBackground(bgColor);
    int componentWidth = targetRegion.width - startX - scale(LEFT_INSET) - scale(RIGHT_INSET);
    int componentHeight = filledHeight - scale(TOP_BOTTOM_INSETS) * 2;
    if (componentWidth > 0 && componentHeight > 0) {
      component.setSize(componentWidth, componentHeight);
      Graphics dg = g.create(startX + scale(LEFT_INSET), filledStartY + arcSize, componentWidth, componentHeight);
      GraphicsUtil.setupAntialiasing(dg);
      component.paint(dg);
      dg.dispose();
    }
  }

  @Override
  public GutterIconRenderer calcGutterIconRenderer(@NotNull Inlay inlay) {
    return myItem.highlighter.getGutterIconRenderer();
  }

  @Override
  public ActionGroup getContextMenuGroup(@NotNull Inlay inlay) {
    return new DefaultActionGroup(Objects.requireNonNull(myItem.highlighter.getGutterIconRenderer()).getClickAction(),
                                  new DocRenderItem.ChangeFontSize());
  }

  private static int getTopMargin() {
    return Registry.intValue("editor.render.doc.comments.top.margin");
  }

  private static int getBottomMargin() {
    return Registry.intValue("editor.render.doc.comments.bottom.margin");
  }

  private static int scale(int value) {
    return (int)(value * UISettings.getDefFontScale());
  }

  static int calcInlayWidth(@NotNull Editor editor) {
    int availableWidth = editor.getScrollingModel().getVisibleArea().width;
    if (availableWidth <= 0) {
      // if editor is not shown yet, we create the inlay with maximum possible width,
      // assuming that there's a higher probability that editor will be shown with larger width than with smaller width
      return MAX_WIDTH;
    }
    return Math.max(scale(MIN_WIDTH), Math.min(scale(MAX_WIDTH), availableWidth));
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
      Map<TextAttribute, Object> fontAttributes = new HashMap<>();
      fontAttributes.put(TextAttribute.SIZE, JBUIScale.scale(DocumentationComponent.getQuickDocFontSize().getSize()));
      // disable kerning for now - laying out all fragments in a file with it takes too much time
      fontAttributes.put(TextAttribute.KERNING, 0);
      myPane.setFont(myPane.getFont().deriveFont(fontAttributes));
      myPane.setForeground(getColorFromRegistry("editor.render.doc.comments.fg"));
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

  private static Color getColorFromRegistry(String key) {
    String[] values = Registry.stringValue(key).split(",");
    try {
      return new JBColor(ColorUtil.fromHex(values[0]), ColorUtil.fromHex(values[1]));
    }
    catch (Exception e) {
      return null;
    }
  }
}
