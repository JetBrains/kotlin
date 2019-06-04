// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.bookmarks;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.reference.SoftReference;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.RetrievableIcon;
import com.intellij.util.IconUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.JBCachingScalableIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import static com.intellij.ide.ui.UISettings.setupAntialiasing;
import static com.intellij.ui.scale.ScaleType.OBJ_SCALE;

public class Bookmark implements Navigatable, Comparable<Bookmark> {
  static final Icon DEFAULT_ICON = new MyCheckedIcon();

  private final VirtualFile myFile;
  @NotNull
  private OpenFileDescriptor myTarget;
  private final Project myProject;
  private Reference<RangeHighlighterEx> myHighlighterRef;

  @NotNull
  private String myDescription;
  private char myMnemonic;
  int index; // index in the list of bookmarks in the Navigate|Bookmarks|show

  public Bookmark(@NotNull Project project, @NotNull VirtualFile file, int line, @NotNull String description) {
    myFile = file;
    myProject = project;
    myDescription = description;

    myTarget = new OpenFileDescriptor(project, file, line, -1, true);

    addHighlighter();
  }

  @NotNull
  public static Font getBookmarkFont() {
    return EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
  }

  @Override
  public int compareTo(@NotNull Bookmark o) {
    int i = myMnemonic != 0 ? o.myMnemonic != 0 ? myMnemonic - o.myMnemonic : -1: o.myMnemonic != 0 ? 1 : 0;
    if (i != 0) return i;
    i = myProject.getName().compareTo(o.myProject.getName());
    if (i != 0) return i;
    i = myFile.getName().compareTo(o.getFile().getName());
    if (i != 0) return i;
    return getTarget().compareTo(o.getTarget());
  }

  void updateHighlighter() {
    release();
    addHighlighter();
  }

  private void addHighlighter() {
    Document document = getCachedDocument();
    if (document != null) {
      createHighlighter((MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true));
    }
  }

  public RangeHighlighter createHighlighter(@NotNull MarkupModelEx markup) {
    final RangeHighlighterEx highlighter;
    int line = getLine();
    if (line >= 0) {
      highlighter = markup.addPersistentLineHighlighter(line, HighlighterLayer.ERROR + 1, null);
      if (highlighter != null) {
        highlighter.setGutterIconRenderer(new MyGutterIconRenderer(this));

        TextAttributes textAttributes =
          ObjectUtils.notNull(EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.BOOKMARKS_ATTRIBUTES),
                              new TextAttributes());
        Color stripeColor = ObjectUtils.notNull(textAttributes.getErrorStripeColor(), new JBColor(0x000000, 0xdbdbdb));
        highlighter.setErrorStripeMarkColor(stripeColor);
        highlighter.setErrorStripeTooltip(getBookmarkTooltip());

        TextAttributes attributes = highlighter.getTextAttributes();
        if (attributes == null) {
          attributes = new TextAttributes();
        }
        attributes.setBackgroundColor(textAttributes.getBackgroundColor());
        attributes.setForegroundColor(textAttributes.getForegroundColor());
        highlighter.setTextAttributes(attributes);
      }
    }
    else {
      highlighter = null;
    }
    myHighlighterRef = highlighter == null ? null : new WeakReference<>(highlighter);
    return highlighter;
  }

  @Deprecated
  @Nullable
  public Document getDocument() {
    return getCachedDocument();
  }

  Document getCachedDocument() {
    return FileDocumentManager.getInstance().getCachedDocument(getFile());
  }

  public void release() {
    int line = getLine();
    if (line < 0) {
      return;
    }
    final Document document = getCachedDocument();
    if (document == null) return;
    MarkupModelEx markup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true);
    final Document markupDocument = markup.getDocument();
    if (markupDocument.getLineCount() <= line) return;
    RangeHighlighterEx highlighter = findMyHighlighter();
    if (highlighter != null) {
      myHighlighterRef = null;
      highlighter.dispose();
    }
  }

  private RangeHighlighterEx findMyHighlighter() {
    final Document document = getCachedDocument();
    if (document == null) return null;
    RangeHighlighterEx result = SoftReference.dereference(myHighlighterRef);
    if (result != null) {
      return result;
    }
    MarkupModelEx markup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true);
    final Document markupDocument = markup.getDocument();
    final int startOffset = 0;
    final int endOffset = markupDocument.getTextLength();

    final Ref<RangeHighlighterEx> found = new Ref<>();
    markup.processRangeHighlightersOverlappingWith(startOffset, endOffset, highlighter -> {
      GutterMark renderer = highlighter.getGutterIconRenderer();
      if (renderer instanceof MyGutterIconRenderer && ((MyGutterIconRenderer)renderer).myBookmark == this) {
        found.set(highlighter);
        return false;
      }
      return true;
    });
    result = found.get();
    myHighlighterRef = result == null ? null : new WeakReference<>(result);
    return result;
  }

  public Icon getIcon() {
    return myMnemonic == 0 ? DEFAULT_ICON : MnemonicIcon.getIcon(myMnemonic);
  }

  @NotNull
  public String getDescription() {
    return myDescription;
  }

  public void setDescription(@NotNull String description) {
    myDescription = description;
  }

  public char getMnemonic() {
    return myMnemonic;
  }

  public void setMnemonic(char mnemonic) {
    myMnemonic = Character.toUpperCase(mnemonic);
  }

  @NotNull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nullable
  String nullizeEmptyDescription() {
    return StringUtil.nullize(myDescription);
  }

  public boolean isValid() {
    if (!getFile().isValid()) {
      return false;
    }
    if (getLine() ==-1) {
      return true;
    }
    RangeHighlighterEx highlighter = findMyHighlighter();
    return highlighter != null && highlighter.isValid();
  }

  @Override
  public boolean canNavigate() {
    return getTarget().canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return getTarget().canNavigateToSource();
  }

  @Override
  public void navigate(boolean requestFocus) {
    getTarget().navigate(requestFocus);
  }

  public int getLine() {
    int targetLine = myTarget.getLine();
    if (targetLine == -1) return -1;
    //What user sees in gutter
    RangeHighlighterEx highlighter = findMyHighlighter();
    if (highlighter != null && highlighter.isValid()) {
      Document document = highlighter.getDocument();
      return document.getLineNumber(highlighter.getStartOffset());
    }
    RangeMarker marker = myTarget.getRangeMarker();
    if (marker != null && marker.isValid()) {
      Document document = marker.getDocument();
      return document.getLineNumber(marker.getStartOffset());
    }
    return targetLine;
  }

  @NotNull
  private OpenFileDescriptor getTarget() {
    int line = getLine();
    if (line != myTarget.getLine()) {
      myTarget = new OpenFileDescriptor(myProject, myFile, line, -1, true);
    }
    return myTarget;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(getQualifiedName());
    String text = nullizeEmptyDescription();
    String description = text == null ? null : StringUtil.escapeXmlEntities(text);
    if (description != null) {
      result.append(": ").append(description);
    }
    return result.toString();
  }

  @NotNull
  public String getQualifiedName() {
    String presentableUrl = myFile.getPresentableUrl();
    if (myFile.isDirectory()) return presentableUrl;

    final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);

    if (psiFile == null) return presentableUrl;

    StructureViewBuilder builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(psiFile);
    if (builder instanceof TreeBasedStructureViewBuilder) {
      StructureViewModel model = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(null);
      Object element;
      try {
        element = model.getCurrentEditorElement();
      }
      finally {
        Disposer.dispose(model);
      }
      if (element instanceof NavigationItem) {
        ItemPresentation presentation = ((NavigationItem)element).getPresentation();
        if (presentation != null) {
          presentableUrl = ((NavigationItem)element).getName() + " " + presentation.getLocationString();
        }
      }
    }

    return IdeBundle.message("bookmark.file.X.line.Y", presentableUrl, getLine() + 1);
  }

  @NotNull
  private String getBookmarkTooltip() {
    StringBuilder result = new StringBuilder("Bookmark");
    if (myMnemonic != 0) {
      result.append(" ").append(myMnemonic);
    }
    String text = nullizeEmptyDescription();
    String description = text == null ? null : StringUtil.escapeXmlEntities(text);
    if (description != null) {
      result.append(": ").append(description);
    }
    return result.toString();
  }

  static class MnemonicIcon extends JBCachingScalableIcon<MnemonicIcon> {
    private static final MnemonicIcon[] cache = new MnemonicIcon[36];//0..9  + A..Z
    private final char myMnemonic;

    @NotNull
    @Override
    public MnemonicIcon copy() {
      return new MnemonicIcon(myMnemonic);
    }

    @NotNull
    static MnemonicIcon getIcon(char mnemonic) {
      int index = mnemonic - 48;
      if (index > 9)
        index -= 7;
      if (index < 0 || index > cache.length-1)
        return new MnemonicIcon(mnemonic);
      if (cache[index] == null)
        cache[index] = new MnemonicIcon(mnemonic);
      return cache[index];
    }

    private MnemonicIcon(char mnemonic) {
      myMnemonic = mnemonic;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      int width = getIconWidth();
      int height = getIconHeight();

      g.setColor(new JBColor(() -> {
        //noinspection UseJBColor
        return !darkBackground() ? new Color(0xffffcc) : new Color(0x675133);
      }));
      g.fillRect(x, y, width, height);

      g.setColor(JBColor.GRAY);
      g.drawRect(x, y, width, height);

      g.setColor(EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground());
      setupAntialiasing(g);

      float startingFontSize = 40f;  // large font for smaller rounding error
      Font font = getBookmarkFont().deriveFont(startingFontSize);
      FontRenderContext fontRenderContext = ((Graphics2D)g).getFontRenderContext();
      double height40 = font.createGlyphVector(fontRenderContext, new char[]{'A'}).getVisualBounds().getHeight();
      font = font.deriveFont((float)(startingFontSize * height / height40 * 0.7));

      GlyphVector gv = font.createGlyphVector(fontRenderContext, new char[]{myMnemonic});
      Rectangle2D bounds = gv.getVisualBounds();
      ((Graphics2D)g).drawGlyphVector(gv, (float)(x + (width - bounds.getWidth())/2 - bounds.getX()),
                                      (float)(y + (height - bounds.getHeight())/2 - bounds.getY()));
    }

    @Override
    public int getIconWidth() {
      return scale(DEFAULT_ICON.getIconWidth());
    }

    private int scale(int width) {
      return (int)Math.ceil(scaleVal(width, OBJ_SCALE));
    }

    @Override
    public int getIconHeight() {
      return scale(DEFAULT_ICON.getIconHeight());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MnemonicIcon that = (MnemonicIcon)o;

      return myMnemonic == that.myMnemonic;
    }

    @Override
    public int hashCode() {
      return myMnemonic;
    }
  }

  private static class MyCheckedIcon extends JBCachingScalableIcon<MyCheckedIcon> implements RetrievableIcon {
    @NotNull
    @Override
    public Icon retrieveIcon() {
      return IconUtil.scale(PlatformIcons.CHECK_ICON, null, getScale());
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      IconUtil.scale(PlatformIcons.CHECK_ICON, c, getScale()).paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
      return scale(PlatformIcons.CHECK_ICON.getIconWidth());
    }

    private int scale(int width) {
      return (int)Math.ceil(scaleVal(width, OBJ_SCALE));
    }

    @Override
    public int getIconHeight() {
      return scale(PlatformIcons.CHECK_ICON.getIconHeight());
    }

    @NotNull
    @Override
    public MyCheckedIcon copy() {
      return new MyCheckedIcon();
    }
  }

  private static boolean darkBackground() {
    Color gutterBackground = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.GUTTER_BACKGROUND);
    if (gutterBackground == null) {
      gutterBackground = EditorColors.GUTTER_BACKGROUND.getDefaultColor();
    }
    return ColorUtil.isDark(gutterBackground);
  }

  private static class MyGutterIconRenderer extends GutterIconRenderer implements DumbAware {
    private final Bookmark myBookmark;

    MyGutterIconRenderer(@NotNull Bookmark bookmark) {
      myBookmark = bookmark;
    }

    @Override
    @NotNull
    public Icon getIcon() {
      return myBookmark.getIcon();
    }

    @Override
    @NotNull
    public String getTooltipText() {
      return myBookmark.getBookmarkTooltip();
    }

    @NotNull
    @Override
    public GutterDraggableObject getDraggableObject() {
      return new GutterDraggableObject() {
        @Override
        public boolean copy(int line, VirtualFile file, int actionId) {
          myBookmark.myTarget = new OpenFileDescriptor(myBookmark.myProject, file, line, -1, true);
          myBookmark.updateHighlighter();
          return true;
        }

        @Override
        public Cursor getCursor(int line, int actionId) {
          return DragSource.DefaultMoveDrop;
        }
      };
    }

    @NotNull
    @Override
    public String getAccessibleName() {
      return "icon: bookmark " + myBookmark.myMnemonic;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MyGutterIconRenderer &&
             Comparing.equal(getTooltipText(), ((MyGutterIconRenderer)obj).getTooltipText()) &&
             Comparing.equal(getIcon(), ((MyGutterIconRenderer)obj).getIcon());
    }

    @Override
     public int hashCode() {
      return getIcon().hashCode();
    }
  }
}
