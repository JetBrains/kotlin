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
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Graphics2DDelegate;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.ImageObserver;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class DocRenderer implements EditorCustomElementRenderer {
  private static final Logger LOG = Logger.getInstance(DocRenderer.class);
  private static final DocRendererMemoryManager MEMORY_MANAGER = new DocRendererMemoryManager();
  private static final DocRenderImageManager IMAGE_MANAGER = new DocRenderImageManager();

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
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new CopySelection());
    group.addSeparator();
    group.add(myItem.createToggleAction());
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_TOGGLE_RENDERED_DOC_FOR_ALL));
    group.add(new DocRenderItem.ChangeFontSize());
    return group;
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
      clearCachedComponent();
      myPane = new EditorPane();
      myPane.setEditable(false);
      myPane.getCaret().setSelectionVisible(true);
      myPane.putClientProperty("caretWidth", 0); // do not reserve space for caret (making content one pixel narrower than component)
      myPane.setEditorKit(createEditorKit(editor));
      myPane.setBorder(JBUI.Borders.empty());
      Map<TextAttribute, Object> fontAttributes = new HashMap<>();
      fontAttributes.put(TextAttribute.SIZE, JBUIScale.scale(DocumentationComponent.getQuickDocFontSize().getSize()));
      // disable kerning for now - laying out all fragments in a file with it takes too much time
      fontAttributes.put(TextAttribute.KERNING, 0);
      myPane.setFont(myPane.getFont().deriveFont(fontAttributes));
      Color textColor = getTextColor(editor.getColorsScheme());
      myPane.setForeground(textColor);
      myPane.setSelectedTextColor(textColor);
      myPane.setSelectionColor(editor.getSelectionModel().getTextAttributes().getBackgroundColor());
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
      myPane.getDocument().putProperty("imageCache", IMAGE_MANAGER.getImageProvider());
      myContentUpdateNeeded = false;
    }
    AppUIUtil.targetToDevice(myPane, editor.getContentComponent());
    myPane.setSize(width, 10_000_000 /* Arbitrary large value, that doesn't lead to overflows and precision loss */);
    if (newInstance) {
      myPane.getPreferredSize(); // trigger internal layout, so that image elements are created
                                 // this is done after 'targetToDevice' call to take correct graphics context into account
      myPane.startImageTracking();
    }
    return myPane;
  }

  void clearCachedComponent() {
    if (myPane != null) {
      myPane.dispose();
      myPane = null;
    }
  }

  void dispose() {
    clearCachedComponent();
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

  private static EditorKit createEditorKit(@NotNull Editor editor) {
    HTMLEditorKit editorKit = new MyEditorKit();
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
        "li p {padding-top: 0}" +
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
    private final List<Image> myImages = new ArrayList<>();
    private final AtomicBoolean myUpdateScheduled = new AtomicBoolean();
    private final AtomicBoolean myRepaintScheduled = new AtomicBoolean();
    private final ImageObserver myImageObserver = new ImageObserver() {
      @Override
      public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        if ((infoflags & (ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0) {
          scheduleUpdate();
          return false;
        }
        return true;
      }
    };
    private boolean myRepaintRequested;

    EditorPane() {
      MEMORY_MANAGER.register(DocRenderer.this, 50 /* rough size estimation */);
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
      myRepaintRequested = true;
    }

    void doWithRepaintTracking(Runnable task) {
      myRepaintRequested = false;
      task.run();
      if (myRepaintRequested) repaintInlay();
    }

    private void repaintInlay() {
      Inlay<DocRenderer> inlay = myItem.inlay;
      if (inlay != null) inlay.repaint();
    }

    @Override
    public void paint(Graphics g) {
      MEMORY_MANAGER.notifyPainted(DocRenderer.this);
      for (Image image : myImages) {
        IMAGE_MANAGER.notifyPainted(image);
      }
      super.paint(g);
    }

    Editor getEditor() {
      return myItem.editor;
    }

    void removeSelection() {
      doWithRepaintTracking(() -> select(0, 0));
    }

    boolean hasSelection() {
      return getSelectionStart() != getSelectionEnd();
    }

    @Nullable Point getSelectionPositionInEditor() {
      if (myPane != this ||
          myItem.inlay == null ||
          myItem.inlay.getRenderer() != DocRenderer.this) {
        return null;
      }
      Rectangle inlayBounds = myItem.inlay.getBounds();
      if (inlayBounds == null) {
        return null;
      }
      Rectangle boundsWithinInlay = getEditorPaneBoundsWithinInlay(myItem.inlay);
      Rectangle locationInPane;
      try {
        locationInPane = modelToView(getSelectionStart());
      }
      catch (BadLocationException e) {
        LOG.error(e);
        locationInPane = new Rectangle();
      }
      return new Point(inlayBounds.x + boundsWithinInlay.x + locationInPane.x, inlayBounds.y + boundsWithinInlay.y + locationInPane.y);
    }

    private void scheduleUpdate() {
      if (myUpdateScheduled.compareAndSet(false, true)) {
        SwingUtilities.invokeLater(() -> {
          myRepaintScheduled.set(false);
          myUpdateScheduled.set(false);
          Inlay<DocRenderer> inlay = myItem.inlay;
          if (this == myPane && inlay != null) {
            DocRenderItemUpdater.getInstance().updateInlays(Collections.singleton(inlay), false);
          }
        });
      }
    }

    private void scheduleRepaint() {
      if (!myUpdateScheduled.get() && myRepaintScheduled.compareAndSet(false, true)) {
        SwingUtilities.invokeLater(() -> {
          myRepaintScheduled.set(false);
          if (this == myPane) {
            repaintInlay();
          }
        });
      }
    }

    void startImageTracking() {
      collectImages(getUI().getRootView(this));
      boolean update = false;
      for (Image image : myImages) {
        IMAGE_MANAGER.setCompletionListener(image, this::scheduleRepaint);
        update |= image.getWidth(myImageObserver) >= 0 || image.getHeight(myImageObserver) >= 0;
      }
      if (update) {
        myImageObserver.imageUpdate(null, ImageObserver.WIDTH | ImageObserver.HEIGHT, 0, 0, 0, 0);
      }
    }

    private void collectImages(View view) {
      if (view instanceof ImageView) {
        Image image = ((ImageView)view).getImage();
        if (image != null) {
          myImages.add(image);
        }
      }
      int childCount = view.getViewCount();
      for (int i = 0; i < childCount; i++) {
        collectImages(view.getView(i));
      }
    }

    void dispose() {
      MEMORY_MANAGER.unregister(DocRenderer.this);
      myImages.forEach(image -> IMAGE_MANAGER.dispose(image));
    }
  }

  private static class MyEditorKit extends JBHtmlEditorKit {
    @Override
    public ViewFactory getViewFactory() {
      return MyViewFactory.INSTANCE;
    }
  }

  private static class MyViewFactory extends JBHtmlEditorKit.JBHtmlFactory {
    private static final MyViewFactory INSTANCE = new MyViewFactory();

    @Override
    public View create(Element elem) {
      View view = super.create(elem);
      return view instanceof ImageView ? new MyScalingImageView(elem) : view;
    }
  }

  private static class MyScalingImageView extends ImageView {
    private int myAvailableWidth;

    private MyScalingImageView(Element element) {
      super(element);
    }

    @Override
    public int getResizeWeight(int axis) {
      return 1;
    }

    @Override
    public float getMaximumSpan(int axis) {
      return getPreferredSpan(axis);
    }

    @Override
    public float getPreferredSpan(int axis) {
      float baseSpan = super.getPreferredSpan(axis);
      if (axis == View.X_AXIS) {
        return baseSpan;
      }
      else {
        int availableWidth = getAvailableWidth();
        if (availableWidth <= 0) return baseSpan;
        float baseXSpan = super.getPreferredSpan(View.X_AXIS);
        if (baseXSpan <= 0) return baseSpan;
        if (availableWidth > baseXSpan) {
          availableWidth = (int)baseXSpan;
        }
        if (myAvailableWidth > 0 && availableWidth != myAvailableWidth) {
          preferenceChanged(null, false, true);
        }
        myAvailableWidth = availableWidth;
        return baseSpan * availableWidth / baseXSpan;
      }
    }

    private int getAvailableWidth() {
      for (View v = this; v != null;) {
        View parent = v.getParent();
        if (parent instanceof FlowView) {
          int childCount = parent.getViewCount();
          for (int i = 0; i < childCount; i++) {
            if (parent.getView(i) == v) {
              return ((FlowView)parent).getFlowSpan(i);
            }
          }
        }
        v = parent;
      }
      return 0;
    }

    @Override
    public void paint(Graphics g, Shape a) {
      Rectangle targetRect = (a instanceof Rectangle) ? (Rectangle)a : a.getBounds();
      Graphics scalingGraphics = new Graphics2DDelegate((Graphics2D)g) {
        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
          int maxWidth = Math.max(0, targetRect.width - 2 * (x - targetRect.x)); // assuming left and right insets are the same
          int maxHeight = Math.max(0, targetRect.height - 2 * (y - targetRect.y)); // assuming top and bottom insets are the same
          if (width > maxWidth) {
            height = height * maxWidth / width;
            width = maxWidth;
          }
          if (height > maxHeight) {
            width = width * maxHeight / height;
            height = maxHeight;
          }
          return super.drawImage(img, x, y, width, height, observer);
        }
      };
      super.paint(scalingGraphics, a);
    }
  }

  private class CopySelection extends DumbAwareAction {
    CopySelection() {
      super(CodeInsightBundle.messagePointer("doc.render.copy.action.text"), AllIcons.Actions.Copy);
      AnAction copyAction = ActionManager.getInstance().getAction(IdeActions.ACTION_COPY);
      if (copyAction != null) {
        copyShortcutFrom(copyAction);
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setVisible(myPane != null && myPane.hasSelection());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      String text = myPane == null ? null : myPane.getSelectedText();
      if (!StringUtil.isEmpty(text)) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
      }
    }
  }
}
