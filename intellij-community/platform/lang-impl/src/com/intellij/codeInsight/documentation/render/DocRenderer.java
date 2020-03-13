// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.QuickDocUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ObjectUtils;
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
  private static final int MIN_WIDTH = 350;
  private static final int MAX_WIDTH = 680;
  private static final int LEFT_INSET = 14;
  private static final int RIGHT_INSET = 12;
  private static final int TOP_BOTTOM_INSETS = 4;

  private static StyleSheet ourCachedStyleSheet;
  private static String ourCachedStyleSheetLinkColor = "non-existing";
  private static String ourCachedStyleSheetMonoFont = "non-existing";

  private final DocRenderItem myItem;
  private boolean myRepaintRequested;
  private boolean myContentUpdateNeeded;
  JEditorPane myPane;

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
    JComponent component = getRendererComponent(inlay, width, -1);
    return component.getPreferredSize().height + scale(TOP_BOTTOM_INSETS) * 2 + scale(getTopMargin()) + scale(getBottomMargin());
  }

  @Override
  public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
    int startX = calcInlayStartX();
    int endX = targetRegion.x + targetRegion.width;
    if (startX >= endX) return;
    int topMargin = scale(getTopMargin());
    int bottomMargin = scale(getBottomMargin());
    int filledHeight = targetRegion.height - topMargin - bottomMargin;
    if (filledHeight <= 0) return;
    int filledStartY = targetRegion.y + topMargin;

    g.setColor(((EditorEx)inlay.getEditor()).getBackgroundColor());
    g.fillRect(startX, filledStartY, endX - startX, filledHeight);
    g.setColor(inlay.getEditor().getColorsScheme().getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_GUIDE));
    g.fillRect(startX, filledStartY, scale(getLineWidth()), filledHeight);

    int topBottomInset = scale(TOP_BOTTOM_INSETS);
    int componentWidth = endX - startX - scale(LEFT_INSET) - scale(RIGHT_INSET);
    int componentHeight = filledHeight - topBottomInset * 2;
    if (componentWidth > 0 && componentHeight > 0) {
      JComponent component = getRendererComponent(inlay, componentWidth, componentHeight);
      Graphics dg = g.create(startX + scale(LEFT_INSET), filledStartY + topBottomInset, componentWidth, componentHeight);
      GraphicsUtil.setupAntialiasing(dg);
      component.paint(dg);
      dg.dispose();
    }
  }

  private static int getLineWidth() {
    return Registry.intValue("editor.render.doc.comments.line.width", 2);
  }

  @Override
  public GutterIconRenderer calcGutterIconRenderer(@NotNull Inlay inlay) {
    return DocRenderDummyLineMarkerProvider.isGutterIconEnabled() ? myItem.new MyGutterIconRenderer(AllIcons.Gutter.JavadocEdit) : null;
  }

  @Override
  public ActionGroup getContextMenuGroup(@NotNull Inlay inlay) {
    return new DefaultActionGroup(getToggleAction(), new DocRenderItem.ChangeFontSize());
  }

  private AnAction getToggleAction() {
    return Objects.requireNonNull(myItem.highlighter.getGutterIconRenderer()).getClickAction();
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
    return myItem.editor.offsetToXY(contentStartOffset, false, true).x;
  }

  Point getEditorPaneLocationWithinInlay() {
    return new Point(calcInlayStartX() + scale(LEFT_INSET), scale(getTopMargin()) + scale(TOP_BOTTOM_INSETS));
  }

  private JComponent getRendererComponent(Inlay inlay, int width, int height) {
    boolean newInstance = false;
    EditorEx editor = (EditorEx)inlay.getEditor();
    if (myPane == null || myContentUpdateNeeded) {
      newInstance = true;
      myPane = new JEditorPane() {
        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
          myRepaintRequested = true;
        }
      };
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
      myPane.setBackground(editor.getBackgroundColor());
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
    myPane.setSize(width, height < 0 ? (newInstance ? Integer.MAX_VALUE : myPane.getHeight()) : height);
    if (newInstance) {
      trackImageUpdates(inlay);
    }
    return myPane;
  }

  private static @NotNull Color getTextColor(@NotNull EditorColorsScheme scheme) {
    TextAttributes attributes = scheme.getAttributes(DefaultLanguageHighlighterColors.DOC_COMMENT);
    Color color = attributes == null ? null : attributes.getForegroundColor();
    return color == null ? scheme.getDefaultForeground() : color;
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
      PsiDocCommentBase comment = myItem.getComment();
      PsiElement owner = comment == null ? null : comment.getOwner();
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

  private static JBHtmlEditorKit createEditorKit(@NotNull Editor editor) {
    JBHtmlEditorKit editorKit = new JBHtmlEditorKit(true);
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
        "code {font-family:\"" + escapedFontName + "\"}" +
        "pre {font-family:\"" + escapedFontName + "\"}" +
        "h1, h2, h3, h4, h5, h6 { margin-top: 0; padding-top: 1px; }" +
        "a { color: #" + linkColorHex + "; text-decoration: none;}" +
        "p { padding: 1px 0 2px 0; }" +
        "ol { padding: 0 16px 0 0; }" +
        "ul { padding: 0 16px 0 0; }" +
        "li { padding: 1px 0 2px 0; }" +
        "table p { padding-bottom: 0}" +
        "td { margin: 4px 0 0 0; padding: 0; }" +
        "th { text-align: left; }"
      );
      ourCachedStyleSheetLinkColor = linkColorHex;
      ourCachedStyleSheetMonoFont = editorFontName;
    }
    return ourCachedStyleSheet;
  }
}
