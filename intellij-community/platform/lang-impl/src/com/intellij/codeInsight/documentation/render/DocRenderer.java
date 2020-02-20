// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.QuickDocUtil;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.ImageObserver;
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

  private static StyleSheet ourCachedStyleSheet;
  private static String ourCachedStyleSheetLaf = "non-existing";
  private static String ourCachedStyleSheetFont = "non-existing";

  private final DocRenderItem myItem;
  private boolean myRepaintRequested;
  JEditorPane myPane;

  DocRenderer(DocRenderItem item) {
    myItem = item;
  }

  @Override
  public int calcWidthInPixels(@NotNull Inlay inlay) {
    return calcInlayWidth(inlay.getEditor());
  }

  @Override
  public int calcHeightInPixels(@NotNull Inlay inlay) {
    int width = Math.max(0, calcInlayWidth(inlay.getEditor()) - calcInlayStartX() - scale(LEFT_INSET) - scale(RIGHT_INSET));
    JComponent component = getRendererComponent(inlay, width, -1);
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

    int componentWidth = targetRegion.width - startX - scale(LEFT_INSET) - scale(RIGHT_INSET);
    int componentHeight = filledHeight - scale(TOP_BOTTOM_INSETS) * 2;
    if (componentWidth > 0 && componentHeight > 0) {
      JComponent component = getRendererComponent(inlay, componentWidth, componentHeight);
      component.setBackground(bgColor);
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

  Point getEditorPaneLocationWithinInlay() {
    return new Point(calcInlayStartX() + scale(LEFT_INSET), scale(getTopMargin()) + scale(ARC_WIDTH));
  }

  private JComponent getRendererComponent(Inlay inlay, int width, int height) {
    boolean newInstance = false;
    if (myPane == null || Boolean.TRUE.equals(inlay.getUserData(RECREATE_COMPONENT) != null)) {
      newInstance = true;
      myPane = new JEditorPane() {
        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
          myRepaintRequested = true;
        }
      };
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
      myPane.addHyperlinkListener(e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          activateLink(e);
        }
      });
      inlay.putUserData(RECREATE_COMPONENT, null);
    }
    AppUIUtil.targetToDevice(myPane, inlay.getEditor().getContentComponent());
    myPane.setSize(width, height < 0 ? (newInstance ? Integer.MAX_VALUE : myPane.getHeight()) : height);
    if (newInstance) {
      trackImageUpdates(inlay);
    }
    return myPane;
  }

  private void activateLink(HyperlinkEvent event) {
    Editor editor = myItem.editor;
    Project project = editor.getProject();
    Element element = event.getSourceElement();
    if (project != null && element != null) {
      Rectangle location = null;
      try {
        location = myPane.modelToView(element.getStartOffset());
      }
      catch (BadLocationException ignored) {}
      PsiElement owner = myItem.getOwner();
      if (owner != null && location != null) {
        DocumentationManager documentationManager = DocumentationManager.getInstance(project);
        if (QuickDocUtil.getActiveDocComponent(project) == null) {
          Point inlayPosition = Objects.requireNonNull(myItem.inlay.getBounds()).getLocation();
          Point relativePosition = getEditorPaneLocationWithinInlay();
          editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POINT,
                             new Point(inlayPosition.x + relativePosition.x + location.x,
                                       inlayPosition.y + relativePosition.y + location.y + location.height));
          documentationManager.showJavaDocInfo(editor, owner, owner, () -> {
            editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POINT, null);
          }, "", false, true);
        }
        DocumentationComponent component = QuickDocUtil.getActiveDocComponent(project);
        if (component != null) {
          if (!documentationManager.hasActiveDockedDocWindow()) {
            component.startWait();
          }
          documentationManager.navigateByLink(component, event.getDescription());
        }
        if (documentationManager.getDocInfoHint() == null) {
          editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POINT, null);
        }
        if (documentationManager.hasActiveDockedDocWindow()) {
          documentationManager.setAllowContentUpdateFromContext(false);
          Disposable disposable = Disposer.newDisposable();
          editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent e) {
              documentationManager.resetAutoUpdateState();
              Disposer.dispose(disposable);
            }
          }, disposable);
        }
      }
    }
  }

  private void trackImageUpdates(Inlay inlay) {
    myPane.getPreferredSize(); // trigger internal layout
    ImageObserver observer = (img, infoflags, x, y, width, height) -> {
      SwingUtilities.invokeLater(() -> {
        if (inlay.isValid()) inlay.update();
      });
      return true;
    };
    if (trackImageUpdates(myPane.getUI().getRootView(myPane), observer)) {
      observer.imageUpdate(null, 0, 0, 0, 0, 0);
    }
  }

  private static boolean trackImageUpdates(View view, ImageObserver observer) {
    boolean result = false;
    if (view instanceof ImageView) {
      Image image = ((ImageView)view).getImage();
      if (image != null) {
        result = image.getWidth(observer) >= 0 || image.getHeight(observer) >= 0;
      }
    }
    int childCount = view.getViewCount();
    for (int i = 0; i < childCount; i++) {
      result |= trackImageUpdates(view.getView(i), observer);
    }
    return result;
  }

  void doWithRepaintTracking(Runnable task) {
    myRepaintRequested = false;
    task.run();
    Inlay<DocRenderer> inlay = myItem.inlay;
    if (myRepaintRequested && inlay != null) {
      inlay.repaint();
    }
  }

  private static JBHtmlEditorKit createEditorKit() {
    JBHtmlEditorKit editorKit = new JBHtmlEditorKit(true);
    editorKit.getStyleSheet().addStyleSheet(getStyleSheet());
    return editorKit;
  }

  private static StyleSheet getStyleSheet() {
    UIManager.LookAndFeelInfo lookAndFeel = LafManager.getInstance().getCurrentLookAndFeel();
    String lafName = lookAndFeel == null ? null : lookAndFeel.getName();
    String editorFontName = EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName();
    if (!Objects.equals(lafName, ourCachedStyleSheetLaf) || !Objects.equals(editorFontName, ourCachedStyleSheetFont)) {
      String escapedFontName = StringUtil.escapeQuotes(editorFontName);
      ourCachedStyleSheet = StartupUiUtil.createStyleSheet(
        "code {font-family:\"" + escapedFontName + "\"}" +
        "pre {font-family:\"" + escapedFontName + "\"}" +
        "h1, h2, h3, h4, h5, h6 { margin-top: 0; padding-top: 1px; }" +
        "a { color: #" + ColorUtil.toHex(JBUI.CurrentTheme.Link.linkColor()) + "; text-decoration: none;}" +
        "p { padding: 1px 0 2px 0; }" +
        "ol { padding: 0 16px 0 0; }" +
        "ul { padding: 0 16px 0 0; }" +
        "li { padding: 1px 0 2px 0; }" +
        "table p { padding-bottom: 0}" +
        "td { margin: 4px 0 0 0; padding: 0; }" +
        "th { text-align: left; }" +
        ".grayed { color: #909090; display: inline;}" +
        ".section { color: #" + ColorUtil.toHex(DocumentationComponent.SECTION_COLOR) + "; padding-right: 4px}"
      );
      ourCachedStyleSheetLaf = lafName;
      ourCachedStyleSheetFont = editorFontName;
    }
    return ourCachedStyleSheet;
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
