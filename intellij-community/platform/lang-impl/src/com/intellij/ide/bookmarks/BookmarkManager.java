// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.bookmarks;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentListener;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.messages.MessageBusConnection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@State(name = "BookmarkManager", storages = {
  @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
})
public final class BookmarkManager implements PersistentStateComponent<Element> {
  private static final int MAX_AUTO_DESCRIPTION_SIZE = 50;
  private final MultiMap<VirtualFile, Bookmark> myBookmarks = MultiMap.createConcurrentSet();
  private final Map<Trinity<VirtualFile, Integer, String>, Bookmark> myDeletedDocumentBookmarks = new HashMap<>();
  private final Map<Document, List<Trinity<Bookmark, Integer, String>>> myBeforeChangeData = new HashMap<>();

  private final Project myProject;

  private boolean mySortedState;
  private final AtomicReference<List<Bookmark>> myPendingState = new AtomicReference<>();

  public static BookmarkManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, BookmarkManager.class);
  }

  public BookmarkManager(@NotNull Project project) {
    myProject = project;
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(EditorColorsManager.TOPIC, __ -> colorsChanged());
    EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
    multicaster.addDocumentListener(new MyDocumentListener(), myProject);
    multicaster.addEditorMouseListener(new MyEditorMouseListener(), myProject);

    mySortedState = UISettings.getInstance().getSortBookmarks();
    connection.subscribe(UISettingsListener.TOPIC, uiSettings -> {
      if (mySortedState != uiSettings.getSortBookmarks()) {
        mySortedState = uiSettings.getSortBookmarks();
        ApplicationManager.getApplication().invokeLater(() -> {
          if (!project.isDisposed()) {
            project.getMessageBus().syncPublisher(BookmarksListener.TOPIC).bookmarksOrderChanged();
          }
        });
      }
    });
  }

  static final class BookmarkManagerPsiDocumentManagerListener implements PsiDocumentListener {
    @Override
    public void documentCreated(@NotNull Document document, @Nullable PsiFile psiFile, @NotNull Project project) {
      VirtualFile file = FileDocumentManager.getInstance().getFile(document);
      if (file == null) {
        return;
      }

      Collection<Bookmark> fileBookmarks = getInstance(project).myBookmarks.get(file);
      if (!fileBookmarks.isEmpty()) {
        AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
          MarkupModelEx markup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
          for (Bookmark bookmark : fileBookmarks) {
            bookmark.createHighlighter(markup);
          }
        });
      }
    }
  }

  public void editDescription(@NotNull Bookmark bookmark, @NotNull JComponent popup) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    String description = Messages.showInputDialog(popup, IdeBundle.message("action.bookmark.edit.description.dialog.message"),
                       IdeBundle.message("action.bookmark.edit.description.dialog.title"), Messages.getQuestionIcon(),
                       bookmark.getDescription(), null);
    if (description != null) {
      setDescription(bookmark, description);
    }
  }

  public void addEditorBookmark(@NotNull Editor editor, int lineIndex) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Document document = editor.getDocument();
    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (psiFile == null) return;

    final VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) return;

    addTextBookmark(virtualFile, lineIndex, getAutoDescription(editor, lineIndex));
  }

  @NotNull
  public Bookmark addTextBookmark(@NotNull VirtualFile file, int lineIndex, @NotNull String description) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    Bookmark b = new Bookmark(myProject, file, lineIndex, description);
    // increment all other indices and put new bookmark at index 0
    myBookmarks.values().forEach(bookmark -> bookmark.index++);
    myBookmarks.putValue(file, b);
    getPublisher().bookmarkAdded(b);
    return b;
  }

  @NotNull
  private BookmarksListener getPublisher() {
    return myProject.getMessageBus().syncPublisher(BookmarksListener.TOPIC);
  }

  @TestOnly
  public void addFileBookmark(@NotNull VirtualFile file, @NotNull String description) {
    if (findFileBookmark(file) != null) {
      return;
    }
    addTextBookmark(file, -1, description);
  }

  @NotNull
  private static String getAutoDescription(@NotNull final Editor editor, final int lineIndex) {
    String autoDescription = editor.getSelectionModel().getSelectedText();
    if (autoDescription == null) {
      Document document = editor.getDocument();
      autoDescription = document.getCharsSequence()
        .subSequence(document.getLineStartOffset(lineIndex), document.getLineEndOffset(lineIndex)).toString().trim();
    }
    if (autoDescription.length() > MAX_AUTO_DESCRIPTION_SIZE) {
      return autoDescription.substring(0, MAX_AUTO_DESCRIPTION_SIZE) + "...";
    }
    return autoDescription;
  }


  @NotNull
  public List<Bookmark> getValidBookmarks() {
    List<Bookmark> answer = ContainerUtil.filter(myBookmarks.values(), b -> b.isValid());
    if (UISettings.getInstance().getSortBookmarks()) {
      Collections.sort(answer);
    }
    else {
      Collections.sort(answer, Comparator.comparingInt(b -> b.index));
    }
    return answer;
  }

  @Nullable
  public Bookmark findEditorBookmark(@NotNull Document document, int line) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null) return null;
    return ContainerUtil.find(myBookmarks.get(file), bookmark -> bookmark.getLine() == line);
  }

  @Nullable
  public Bookmark findFileBookmark(@NotNull VirtualFile file) {
    return ContainerUtil.find(myBookmarks.get(file), bookmark -> bookmark.getLine() == -1);
  }

  @Nullable
  public Bookmark findBookmarkForMnemonic(char m) {
    final char mm = Character.toUpperCase(m);
    return ContainerUtil.find(myBookmarks.values(), bookmark -> bookmark.getMnemonic() == mm);
  }

  public boolean hasBookmarksWithMnemonics() {
    return ContainerUtil.or(myBookmarks.values(), bookmark -> bookmark.getMnemonic() != 0);
  }

  public void removeBookmark(@NotNull Bookmark bookmark) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    VirtualFile file = bookmark.getFile();
    if (myBookmarks.remove(file, bookmark)) {
      int index = bookmark.index;
      // decrement all other indices to maintain them monotonic
      myBookmarks.values().forEach(b -> b.index -= b.index > index ? 1 : 0);
      bookmark.release();
      getPublisher().bookmarkRemoved(bookmark);
    }
  }

  @Override
  public Element getState() {
    Element container = new Element("BookmarkManager");
    writeExternal(container);
    return container;
  }

  @Override
  public void loadState(@NotNull Element state) {
    myPendingState.set(readExternal(state));

    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      ApplicationManager.getApplication().invokeLater(() -> {
        List<Bookmark> newList = myPendingState.getAndSet(null);
        if (newList != null) {
          applyNewState(newList, true);
        }
      }, myProject.getDisposed());
    });
  }

  @TestOnly
  public void applyNewStateInTestMode(@NotNull List<Bookmark> newList) {
    applyNewState(newList, false);
  }

  private void applyNewState(@NotNull List<Bookmark> newList, boolean fireEvents) {
    if (!myBookmarks.isEmpty()) {
      Bookmark[] bookmarks = myBookmarks.values().toArray(new Bookmark[0]);
      for (Bookmark bookmark : bookmarks) {
        bookmark.release();
      }
      myBookmarks.clear();
    }

    int bookmarkIndex = newList.size() - 1;
    List<Bookmark> addedBookmarks = new ArrayList<>(newList.size());
    for (Bookmark bookmark : newList) {
      OpenFileDescriptor target = bookmark.init(myProject);
      if (target == null) {
        continue;
      }

      if (target.getLine() == -1 && findFileBookmark(target.getFile()) != null) {
        continue;
      }

      bookmark.index = bookmarkIndex--;

      char mnemonic = bookmark.getMnemonic();
      if (mnemonic != Character.MIN_VALUE ) {
        Bookmark old = findBookmarkForMnemonic(mnemonic);
        if (old != null) {
          removeBookmark(old);
        }
      }

      myBookmarks.putValue(target.getFile(), bookmark);
      addedBookmarks.add(bookmark);
    }

    if (fireEvents) {
      for (Bookmark bookmark : addedBookmarks) {
        getPublisher().bookmarkAdded(bookmark);
      }
    }
  }

  @NotNull
  private static List<Bookmark> readExternal(@NotNull Element element) {
    List<Bookmark> result = new ArrayList<>();
    for (Element bookmarkElement : element.getChildren("bookmark")) {
      String url = bookmarkElement.getAttributeValue("url");
      if (StringUtil.isEmptyOrSpaces(url)) {
        continue;
      }

      String line = bookmarkElement.getAttributeValue("line");
      String description = StringUtil.notNullize(bookmarkElement.getAttributeValue("description"));
      String mnemonic = bookmarkElement.getAttributeValue("mnemonic");

      int lineIndex = -1;
      if (line != null) {
        try {
          lineIndex = Integer.parseInt(line);
        }
        catch (NumberFormatException ignore) {
          // Ignore. Will miss bookmark if line number cannot be parsed
          continue;
        }
      }
      Bookmark bookmark = new Bookmark(url, lineIndex, description);
      if (mnemonic != null && mnemonic.length() == 1) {
        bookmark.setMnemonic(mnemonic.charAt(0));
      }
      result.add(bookmark);
    }
    return result;
  }

  private void writeExternal(Element element) {
    List<Bookmark> bookmarks = new ArrayList<>(myBookmarks.values());
    // store in reverse order so that loadExternal() will assign them correct indices
    Collections.sort(bookmarks, Comparator.<Bookmark>comparingInt(o -> o.index).reversed());

    for (Bookmark bookmark : bookmarks) {
      if (!bookmark.isValid()) continue;
      Element bookmarkElement = new Element("bookmark");

      bookmarkElement.setAttribute("url", bookmark.getFile().getUrl());

      String description = bookmark.nullizeEmptyDescription();
      if (description != null) {
        bookmarkElement.setAttribute("description", description);
      }

      int line = bookmark.getLine();
      if (line >= 0) {
        bookmarkElement.setAttribute("line", String.valueOf(line));
      }

      char mnemonic = bookmark.getMnemonic();
      if (mnemonic != 0) {
        bookmarkElement.setAttribute("mnemonic", String.valueOf(mnemonic));
      }

      element.addContent(bookmarkElement);
    }
  }

  /**
   * Try to move bookmark one position up in the list
   */
  public void moveBookmarkUp(@NotNull Bookmark bookmark) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final int index = bookmark.index;
    if (index > 0) {
      Bookmark other = ContainerUtil.find(myBookmarks.values(), b -> b.index == index - 1);
      other.index = index;
      bookmark.index = index - 1;
      EventQueue.invokeLater(() -> {
        getPublisher().bookmarkChanged(bookmark);
        getPublisher().bookmarkChanged(other);
      });
    }
  }

  /**
   * Try to move bookmark one position down in the list
   */
  public void moveBookmarkDown(@NotNull Bookmark bookmark) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final int index = bookmark.index;
    if (index < myBookmarks.values().size() - 1) {
      Bookmark other = ContainerUtil.find(myBookmarks.values(), b -> b.index == index + 1);
      other.index = index;
      bookmark.index = index + 1;
      EventQueue.invokeLater(() -> {
        getPublisher().bookmarkChanged(bookmark);
        getPublisher().bookmarkChanged(other);
      });
    }
  }

  @Nullable
  public Bookmark findLineBookmark(@NotNull Editor editor, boolean isWrapped, boolean next) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) return null;
    List<Bookmark> bookmarksForDocument = new ArrayList<>(myBookmarks.get(file));
    if (bookmarksForDocument.isEmpty()) return null;
    int sign = next ? 1 : -1;
    Collections.sort(bookmarksForDocument, (o1, o2) -> sign * (o1.getLine() - o2.getLine()));
    int caretLine = editor.getCaretModel().getLogicalPosition().line;
    for (Bookmark bookmark : bookmarksForDocument) {
      if (next && bookmark.getLine() > caretLine) return bookmark;
      if (!next && bookmark.getLine() < caretLine) return bookmark;
    }
    return isWrapped && !bookmarksForDocument.isEmpty() ? bookmarksForDocument.get(0) : null;
  }

  public void setMnemonic(@NotNull Bookmark bookmark, char c) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final Bookmark old = findBookmarkForMnemonic(c);
    if (old != null) removeBookmark(old);

    bookmark.setMnemonic(c);
    getPublisher().bookmarkChanged(bookmark);
  }

  public void setDescription(@NotNull Bookmark bookmark, @NotNull String description) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    bookmark.setDescription(description);
    getPublisher().bookmarkChanged(bookmark);
  }

  private void colorsChanged() {
    for (Bookmark bookmark : myBookmarks.values()) {
      bookmark.updateHighlighter();
    }
  }

  private class MyEditorMouseListener implements EditorMouseListener {
    @Override
    public void mouseClicked(@NotNull final EditorMouseEvent e) {
      if (e.getArea() != EditorMouseEventArea.LINE_MARKERS_AREA) return;
      if (e.getMouseEvent().isPopupTrigger()) return;
      if ((e.getMouseEvent().getModifiers() & (SystemInfo.isMac ? InputEvent.META_MASK : InputEvent.CTRL_MASK)) == 0) return;

      Editor editor = e.getEditor();
      int line = editor.xyToLogicalPosition(new Point(e.getMouseEvent().getX(), e.getMouseEvent().getY())).line;
      if (line < 0) return;

      Document document = editor.getDocument();

      Bookmark bookmark = findEditorBookmark(document, line);
      if (bookmark == null) {
        addEditorBookmark(editor, line);
      }
      else {
        removeBookmark(bookmark);
      }
      e.consume();
    }
  }

  private final class MyDocumentListener implements DocumentListener {
    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent e) {
      Document doc = e.getDocument();
      VirtualFile file = FileDocumentManager.getInstance().getFile(doc);
      if (file != null) {
        for (Bookmark bookmark : myBookmarks.get(file)) {
          if (bookmark.getLine() == -1) continue;
          List<Trinity<Bookmark, Integer, String>> list = myBeforeChangeData.computeIfAbsent(doc, __ -> new ArrayList<>());
          list.add(new Trinity<>(bookmark,
                                 bookmark.getLine(),
                                 doc.getText(new TextRange(doc.getLineStartOffset(bookmark.getLine()),
                                                           doc.getLineEndOffset(bookmark.getLine())))));
        }
      }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent e) {
      if (!ApplicationManager.getApplication().isDispatchThread()) {
        return;// Changes in lightweight documents are irrelevant to bookmarks and have to be ignored
      }
      VirtualFile file = FileDocumentManager.getInstance().getFile(e.getDocument());
      List<Bookmark> bookmarksToRemove = null;
      if (file != null) {
        for (Bookmark bookmark : myBookmarks.get(file)) {
          if (!bookmark.isValid() || isDuplicate(bookmark, file, bookmarksToRemove)) {
            if (bookmarksToRemove == null) {
              bookmarksToRemove = new ArrayList<>();
            }
            bookmarksToRemove.add(bookmark);
          }
        }
      }

      if (bookmarksToRemove != null) {
        for (Bookmark bookmark : bookmarksToRemove) {
          moveToDeleted(bookmark);
        }
      }

      myBeforeChangeData.remove(e.getDocument());

      for (Iterator<Map.Entry<Trinity<VirtualFile, Integer, String>, Bookmark>> iterator = myDeletedDocumentBookmarks.entrySet().iterator();
           iterator.hasNext(); ) {
        Map.Entry<Trinity<VirtualFile, Integer, String>, Bookmark> entry = iterator.next();

        VirtualFile virtualFile = entry.getKey().first;
        if (!virtualFile.isValid()) {
          iterator.remove();
          continue;
        }

        Bookmark bookmark = entry.getValue();
        Document document = bookmark.getCachedDocument();
        if (document == null || !bookmark.getFile().equals(virtualFile)) {
          continue;
        }
        Integer line = entry.getKey().second;
        if (document.getLineCount() <= line) {
          continue;
        }

        String lineContent = getLineContent(document, line);

        String bookmarkedText = entry.getKey().third;
        //'move statement up' action kills line bookmark: fix for single line movement up/down
        if (!bookmarkedText.equals(lineContent)
            && line > 1
            && (bookmarkedText.equals(StringUtil.trimEnd(e.getNewFragment().toString(), "\n"))
                ||
                bookmarkedText.equals(StringUtil.trimEnd(e.getOldFragment().toString(), "\n")))) {
          line -= 2;
          lineContent = getLineContent(document, line);
        }
        if (bookmarkedText.equals(lineContent) && findEditorBookmark(document, line) == null) {
          Bookmark restored = addTextBookmark(bookmark.getFile(), line, bookmark.getDescription());
          if (bookmark.getMnemonic() != 0) {
            setMnemonic(restored, bookmark.getMnemonic());
          }
          iterator.remove();
        }
      }
    }

    private boolean isDuplicate(Bookmark bookmark, @NotNull VirtualFile file, @Nullable List<Bookmark> toRemove) {
      // it's quadratic but oh well. let's hope users are sane enough not to have thousands of bookmarks in one file.
      for (Bookmark b : myBookmarks.get(file)) {
        if (b == bookmark) continue;
        if (!b.isValid()) continue;
        if (Comparing.equal(b.getFile(), bookmark.getFile()) && b.getLine() == bookmark.getLine()) {
          if (toRemove == null || !toRemove.contains(b)) {
            return true;
          }
        }
      }
      return false;
    }

    private void moveToDeleted(Bookmark bookmark) {
      List<Trinity<Bookmark, Integer, String>> list = myBeforeChangeData.get(bookmark.getCachedDocument());

      if (list != null) {
        for (Trinity<Bookmark, Integer, String> trinity : list) {
          if (trinity.first == bookmark) {
            removeBookmark(bookmark);
            myDeletedDocumentBookmarks.put(new Trinity<>(bookmark.getFile(), trinity.second, trinity.third), bookmark);
            break;
          }
        }
      }
    }

    private String getLineContent(Document document, int line) {
      int start = document.getLineStartOffset(line);
      int end = document.getLineEndOffset(line);
      return document.getText(new TextRange(start, end));
    }
  }
}

