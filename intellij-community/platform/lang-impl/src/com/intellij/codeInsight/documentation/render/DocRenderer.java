// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.QuickDocUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.MouseShortcut;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.ImageObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class DocRenderer implements EditorCustomElementRenderer {
  private static final int MIN_WIDTH = 350;
  private static final int MAX_WIDTH = 680;
  private static final int LEFT_INSET = 14;
  private static final int RIGHT_INSET = 12;
  private static final int TOP_BOTTOM_INSETS = 2;
  private static final int TOP_BOTTOM_MARGINS = 4;
  private static final int LINE_WIDTH = 2;
  private static final int ARC_RADIUS = 5;

  private static StyleSheet ourCachedStyleSheet;
  private static String ourCachedStyleSheetLinkColor = "non-existing";
  private static String ourCachedStyleSheetMonoFont = "non-existing";

  private final DocRenderItem myItem;
  private boolean myContentUpdateNeeded;
  private EditorPane myPane;

  DocRenderer(@NotNull DocRenderItem item) {
    myItem = item;
  }

  void updateContent() {
    Inlay<DocRenderer> inlay = myItem.inlay;
    if (inlay != null) {
      myContentUpdateNeeded = true;
      inlay.update();
    }
  }

  @Override
  public int calcWidthInPixels(@NotNull Inlay inlay) {
    return calcInlayWidth(inlay.getEditor());
  }

  @Override
  public int calcHeightInPixels(@NotNull Inlay inlay) {
    Editor editor = inlay.getEditor();
    int width = Math.max(0, calcInlayWidth(editor) - calcInlayStartX() + editor.getInsets().left - scale(LEFT_INSET) - scale(RIGHT_INSET));
    JComponent component = getRendererComponent(inlay, width);
    return Math.max(editor.getLineHeight(),
                    component.getPreferredSize().height + scale(TOP_BOTTOM_INSETS) * 2 + scale(TOP_BOTTOM_MARGINS) * 2);
  }

  @Override
  public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
    int startX = calcInlayStartX();
    int endX = targetRegion.x + targetRegion.width;
    if (startX >= endX) return;
    int margin = scale(TOP_BOTTOM_MARGINS);
    int filledHeight = targetRegion.height - margin * 2;
    if (filledHeight <= 0) return;
    int filledStartY = targetRegion.y + margin;

    EditorEx editor = (EditorEx)inlay.getEditor();
    Color defaultBgColor = editor.getBackgroundColor();
    Color currentBgColor = textAttributes.getBackgroundColor();
    Color bgColor = currentBgColor == null ? defaultBgColor
                                           : ColorUtil.mix(defaultBgColor, textAttributes.getBackgroundColor(), .5);
    if (currentBgColor != null) {
      g.setColor(bgColor);
      int arcDiameter = ARC_RADIUS * 2;
      if (endX - startX >= arcDiameter) {
        g.fillRect(startX, filledStartY, endX - startX - ARC_RADIUS, filledHeight);
        Object savedHint = ((Graphics2D)g).getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillRoundRect(endX - arcDiameter, filledStartY, arcDiameter, filledHeight, arcDiameter, arcDiameter);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedHint);
      }
      else {
        g.fillRect(startX, filledStartY, endX - startX, filledHeight);
      }
    }
    g.setColor(editor.getColorsScheme().getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_GUIDE));
    g.fillRect(startX, filledStartY, scale(LINE_WIDTH), filledHeight);

    int topBottomInset = scale(TOP_BOTTOM_INSETS);
    int componentWidth = endX - startX - scale(LEFT_INSET) - scale(RIGHT_INSET);
    int componentHeight = filledHeight - topBottomInset * 2;
    if (componentWidth > 0 && componentHeight > 0) {
      JComponent component = getRendererComponent(inlay, componentWidth);
      component.setBackground(bgColor);
      Graphics dg = g.create(startX + scale(LEFT_INSET), filledStartY + topBottomInset, componentWidth, componentHeight);
      UISettings.setupAntialiasing(dg);
      component.paint(dg);
      dg.dispose();
    }
  }

  @Override
  public GutterIconRenderer calcGutterIconRenderer(@NotNull Inlay inlay) {
    DocRenderItem.MyGutterIconRenderer highlighterIconRenderer =
      (DocRenderItem.MyGutterIconRenderer)myItem.highlighter.getGutterIconRenderer();
    return highlighterIconRenderer == null ? null : myItem.new MyGutterIconRenderer(AllIcons.Gutter.JavadocEdit,
                                                                                    highlighterIconRenderer.isIconVisible());
  }

  @Override
  public ActionGroup getContextMenuGroup(@NotNull Inlay inlay) {
    return new DefaultActionGroup(myItem.createToggleAction(), new DocRenderItem.ChangeFontSize());
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
    RangeHighlighter highlighter = myItem.highlighter;
    if (!highlighter.isValid()) return 0;
    Document document = myItem.editor.getDocument();
    int lineStartOffset = document.getLineStartOffset(document.getLineNumber(highlighter.getEndOffset()) + 1);
    int contentStartOffset = CharArrayUtil.shiftForward(document.getImmutableCharSequence(), lineStartOffset, " \t\n");
    return myItem.editor.offsetToXY(contentStartOffset, false, true).x;
  }

  Rectangle getEditorPaneBoundsWithinInlay(Inlay inlay) {
    int relativeX = calcInlayStartX() - myItem.editor.getInsets().left + scale(LEFT_INSET);
    int relativeY = scale(TOP_BOTTOM_MARGINS) + scale(TOP_BOTTOM_INSETS);
    return new Rectangle(relativeX, relativeY,
                         inlay.getWidthInPixels() - relativeX - scale(RIGHT_INSET), inlay.getHeightInPixels() - relativeY * 2);
  }

  EditorPane getRendererComponent(Inlay inlay, int width) {
    boolean newInstance = false;
    EditorEx editor = (EditorEx)inlay.getEditor();
    if (myPane == null || myContentUpdateNeeded) {
      newInstance = true;
      myPane = new EditorPane();
      myPane.setEditable(false);
      myPane.putClientProperty("caretWidth", 0); // do not reserve space for caret (making content one pixel narrower than component)
      myPane.setEditorKit(createEditorKit(editor));
      myPane.setBorder(JBUI.Borders.empty());
      Map<TextAttribute, Object> fontAttributes = new HashMap<>();
      fontAttributes.put(TextAttribute.SIZE, JBUIScale.scale(DocumentationComponent.getQuickDocFontSize().getSize()));
      // disable kerning for now - laying out all fragments in a file with it takes too much time
      fontAttributes.put(TextAttribute.KERNING, 0);
      myPane.setFont(myPane.getFont().deriveFont(fontAttributes));
      myPane.setForeground(getTextColor(editor.getColorsScheme()));
      UIUtil.enableEagerSoftWrapping(myPane);
      String textToRender = myItem.textToRender;
      if (textToRender == null) {
        textToRender = CodeInsightBundle.message("doc.render.loading.text");
      }
      myPane.setText(textToRender);
      myPane.addHyperlinkListener(e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          activateLink(e);
        }
      });
      myContentUpdateNeeded = false;
    }
    AppUIUtil.targetToDevice(myPane, editor.getContentComponent());
    myPane.setSize(width, 10_000_000 /* Arbitrary large value, that doesn't lead to overflows and precision loss */);
    if (newInstance) {
      trackImageUpdates(inlay, myPane);
    }
    DocRendererMemoryManager.onRendererComponentUsage(this);
    return myPane;
  }

  void clearCachedComponent() {
    myPane = null;
  }

  void dispose() {
    DocRendererMemoryManager.stopTracking(this);
  }

  private static @NotNull Color getTextColor(@NotNull EditorColorsScheme scheme) {
    TextAttributes attributes = scheme.getAttributes(DefaultLanguageHighlighterColors.DOC_COMMENT);
    Color color = attributes == null ? null : attributes.getForegroundColor();
    return color == null ? scheme.getDefaultForeground() : color;
  }

  private void activateLink(HyperlinkEvent event) {
    Element element = event.getSourceElement();
    if (element == null) return;

    Rectangle location = null;
    try {
      location = ((JEditorPane)event.getSource()).modelToView(element.getStartOffset());
    }
    catch (BadLocationException ignored) {}
    if (location == null) return;

    PsiDocCommentBase comment = myItem.getComment();
    if (comment == null) return;

    PsiElement context = ObjectUtils.notNull(comment.getOwner(), comment);
    String url = event.getDescription();
    if (isGotoDeclarationEvent()) {
      navigateToDeclaration(context, url);
    }
    else {
      showDocumentation(myItem.editor, context, url, location);
    }
  }

  private static boolean isGotoDeclarationEvent() {
    KeymapManager keymapManager = KeymapManager.getInstance();
    if (keymapManager == null) return false;
    AWTEvent event = IdeEventQueue.getInstance().getTrueCurrentEvent();
    if (!(event instanceof MouseEvent)) return false;
    MouseShortcut mouseShortcut = KeymapUtil.createMouseShortcut((MouseEvent)event);
    return keymapManager.getActiveKeymap().getActionIds(mouseShortcut).contains(IdeActions.ACTION_GOTO_DECLARATION);
  }

  private static void navigateToDeclaration(@NotNull PsiElement context, @NotNull String linkUrl) {
    PsiElement targetElement = DocumentationManager.getInstance(context.getProject()).getTargetElement(context, linkUrl);
    if (targetElement instanceof Navigatable) {
      ((Navigatable)targetElement).navigate(true);
    }
  }

  private void showDocumentation(@NotNull Editor editor,
                                 @NotNull PsiElement context,
                                 @NotNull String linkUrl,
                                 @NotNull Rectangle linkLocationWithinInlay) {
    Project project = context.getProject();
    DocumentationManager documentationManager = DocumentationManager.getInstance(project);
    if (QuickDocUtil.getActiveDocComponent(project) == null) {
      Inlay<DocRenderer> inlay = myItem.inlay;
      Point inlayPosition = Objects.requireNonNull(inlay.getBounds()).getLocation();
      Rectangle relativeBounds = getEditorPaneBoundsWithinInlay(inlay);
      editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POINT,
                         new Point(inlayPosition.x + relativeBounds.x + linkLocationWithinInlay.x,
                                   inlayPosition.y + relativeBounds.y + linkLocationWithinInlay.y + linkLocationWithinInlay.height));
      documentationManager.showJavaDocInfo(editor, context, context, () -> {
        editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POINT, null);
      }, "", false, true);
    }
    DocumentationComponent component = QuickDocUtil.getActiveDocComponent(project);
    if (component != null) {
      if (!documentationManager.hasActiveDockedDocWindow()) {
        component.startWait();
      }
      documentationManager.navigateByLink(component, linkUrl);
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

  private static void trackImageUpdates(Inlay inlay, JEditorPane editorPane) {
    editorPane.getPreferredSize(); // trigger internal layout
    ImageObserver observer = (img, infoflags, x, y, width, height) -> {
      SwingUtilities.invokeLater(() -> {
        if (inlay.isValid()) inlay.update();
      });
      return true;
    };
    if (trackImageUpdates(editorPane.getUI().getRootView(editorPane), observer)) {
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

  private static JBHtmlEditorKit createEditorKit(@NotNull Editor editor) {
    JBHtmlEditorKit editorKit = new JBHtmlEditorKit();
    editorKit.getStyleSheet().addStyleSheet(getStyleSheet(editor));
    return editorKit;
  }

  private static StyleSheet getStyleSheet(@NotNull Editor editor) {
    EditorColorsScheme colorsScheme = editor.getColorsScheme();
    String editorFontName = ObjectUtils.notNull(colorsScheme.getEditorFontName(), Font.MONOSPACED);
    Color linkColor = colorsScheme.getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_LINK);
    if (linkColor == null) linkColor = getTextColor(colorsScheme);
    String linkColorHex = ColorUtil.toHex(linkColor);
    if (!Objects.equals(linkColorHex, ourCachedStyleSheetLinkColor) || !Objects.equals(editorFontName, ourCachedStyleSheetMonoFont)) {
      String escapedFontName = StringUtil.escapeQuotes(editorFontName);
      ourCachedStyleSheet = StartupUiUtil.createStyleSheet(
        "body {overflow-wrap: anywhere}" + // supported by JetBrains Runtime
        "code {font-family: \"" + escapedFontName + "\"}" +
        "pre {font-family: \"" + escapedFontName + "\";" +
             "white-space: pre-wrap}" + // supported by JetBrains Runtime
        "h1, h2, h3, h4, h5, h6 {margin-top: 0; padding-top: 1}" +
        "a {color: #" + linkColorHex + "; text-decoration: none}" +
        "p {padding: 7 0 2 0}" +
        "ol {padding: 0 20 0 0}" +
        "ul {padding: 0 20 0 0}" +
        "li {padding: 1 0 2 0}" +
        "table p {padding-bottom: 0}" +
        "th {text-align: left}" +
        "td {padding: 2 0 2 0}" +
        "td p {padding-top: 0}" +
        ".sections {border-spacing: 0}" +
        ".section {padding-right: 5; white-space: nowrap}" +
        ".content {padding: 2 0 2 0}"
      );
      ourCachedStyleSheetLinkColor = linkColorHex;
      ourCachedStyleSheetMonoFont = editorFontName;
    }
    return ourCachedStyleSheet;
  }

  class EditorPane extends JEditorPane {
    private boolean myRepaintRequested;

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
      myRepaintRequested = true;
    }

    void doWithRepaintTracking(Runnable task) {
      myRepaintRequested = false;
      task.run();
      Inlay<DocRenderer> inlay = myItem.inlay;
      if (myRepaintRequested && inlay != null) {
        inlay.repaint();
      }
    }

    Editor getEditor() {
      return myItem.editor;
    }
  }
}
