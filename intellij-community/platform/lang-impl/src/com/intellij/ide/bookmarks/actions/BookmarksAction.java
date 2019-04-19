/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.bookmarks.actions;

import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkItem;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.ide.bookmarks.BookmarksListener;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.util.DetailViewImpl;
import com.intellij.ui.popup.util.ItemWrapper;
import com.intellij.ui.popup.util.MasterDetailPopupBuilder;
import com.intellij.ui.speedSearch.FilteringListModel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * @author max
 */
public class BookmarksAction extends AnAction implements DumbAware, MasterDetailPopupBuilder.Delegate {
  private static final String DIMENSION_SERVICE_KEY = "bookmarks";

  private JBPopup myPopup;

  public BookmarksAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getProject() != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    if (myPopup != null && myPopup.isVisible()) {
      myPopup.cancel();
      return;
    }

    DefaultListModel<BookmarkItem> model = buildModel(project);
    JBList<BookmarkItem> list = new JBList<>(model);

    EditBookmarkDescriptionAction editDescriptionAction = new EditBookmarkDescriptionAction(project, list);
    DefaultActionGroup actions = new DefaultActionGroup();
    actions.add(editDescriptionAction);
    actions.add(new DeleteBookmarkAction(project, list));
    actions.add(new ToggleSortBookmarksAction());
    actions.add(new MoveBookmarkUpAction(project, list));
    actions.add(new MoveBookmarkDownAction(project, list));

    JBPopup popup = new MasterDetailPopupBuilder(project).
      setList(list).
      setDelegate(this).
      setDetailView(new MyDetailView(project)).
      setDimensionServiceKey(DIMENSION_SERVICE_KEY).
      setAddDetailViewToEast(true).
      setActionsGroup(actions).
      setPopupTuner(builder -> builder.setCloseOnEnter(false).setCancelOnClickOutside(false)).
      setDoneRunnable(() -> { if (myPopup != null) myPopup.cancel(); }).
      createMasterDetailPopup();

    myPopup = popup;

    new AnAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        for (BookmarkItem item : list.getSelectedValuesList()) {
          if (item != null) {
            itemChosen(item, project, popup, true);
          }
        }
      }
    }.registerCustomShortcutSet(CommonShortcuts.getEditSource(), list, popup);

    editDescriptionAction.setPopup(popup);

    Disposer.register(popup, () -> {
      if (myPopup == popup) {
        myPopup = null;
        editDescriptionAction.setPopup(null);
      }
    });

    Point location = DimensionService.getInstance().getLocation(DIMENSION_SERVICE_KEY, project);
    myPopup.getContent().putClientProperty("BookmarkPopup", "TRUE");
    if (location != null) {
      popup.showInScreenCoordinates(WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow(), location);
    }
    else {
      popup.showInBestPositionFor(e.getDataContext());
    }

    list.getEmptyText().setText("No Bookmarks");
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    project.getMessageBus().connect(popup).subscribe(BookmarksListener.TOPIC, new BookmarksListener() {
      @Override
      public void bookmarksOrderChanged() {
        Set<BookmarkItem> selectedValues = new TreeSet<>(list.getSelectedValuesList());
        DefaultListModel<BookmarkItem> listModel = buildModel(project);
        list.setModel(listModel);
        ListSelectionModel selectionModel = list.getSelectionModel();
        for (int i = 0; i < listModel.getSize(); i++) {
          if (selectedValues.contains(listModel.get(i))) {
            selectionModel.addSelectionInterval(i, i);
          }
        }
      }
    });

    BookmarkInContextInfo info = new BookmarkInContextInfo(e.getDataContext(), project).invoke();
    if (info != null) {
      Bookmark bookmark = info.getBookmarkAtPlace();
      if (bookmark != null) {
        for (int i = 0; i < model.getSize(); i++) {
          BookmarkItem item = model.getElementAt(i);
          if (item != null && item.getBookmark() == bookmark) {
            list.setSelectedValue(item, true);
            break;
          }
        }
      }
    }
  }

  private static DefaultListModel<BookmarkItem> buildModel(Project project) {
    DefaultListModel<BookmarkItem> model = new DefaultListModel<>();
    for (Bookmark bookmark : BookmarkManager.getInstance(project).getValidBookmarks()) {
      model.addElement(new BookmarkItem(bookmark));
    }
    return model;
  }

  @Override
  public String getTitle() {
    return "Bookmarks";
  }

  @Override
  public void handleMnemonic(KeyEvent e, Project project, JBPopup popup) {
    char mnemonic = e.getKeyChar();
    final Bookmark bookmark = BookmarkManager.getInstance(project).findBookmarkForMnemonic(mnemonic);
    if (bookmark != null) {
      popup.cancel();
      IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> bookmark.navigate(true));
    }
  }

  @Override
  @Nullable
  public JComponent createAccessoryView(Project project) {
    if (!BookmarkManager.getInstance(project).hasBookmarksWithMnemonics()) {
      return null;
    }

    JLabel mnemonicLabel = new JLabel() {
      @Override
      public void setFont(Font font) {
        super.setFont(font);
        String oldText = getText();
        try {
          setPreferredSize(null);
          setText("W.");
          setPreferredSize(getPreferredSize());
        } finally {
          setText(oldText);
        }
      }
    };
    mnemonicLabel.setFont(Bookmark.getBookmarkFont());
    mnemonicLabel.setOpaque(false);
    return mnemonicLabel;
  }

  @Override
  public Object[] getSelectedItemsInTree() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public void itemChosen(ItemWrapper item, Project project, JBPopup popup, boolean withEnterOrDoubleClick) {
    if (item instanceof BookmarkItem && withEnterOrDoubleClick) {
      Bookmark bookmark = ((BookmarkItem)item).getBookmark();
      popup.cancel();
      bookmark.navigate(true);
    }
  }

  @Override
  public void removeSelectedItemsInTree() { }

  protected static class BookmarkInContextInfo {
    private final DataContext myDataContext;
    private final Project myProject;
    private Bookmark myBookmarkAtPlace;
    private VirtualFile myFile;
    private int myLine;

    public BookmarkInContextInfo(DataContext dataContext, Project project) {
      myDataContext = dataContext;
      myProject = project;
    }

    public Bookmark getBookmarkAtPlace() {
      return myBookmarkAtPlace;
    }

    public VirtualFile getFile() {
      return myFile;
    }

    public int getLine() {
      return myLine;
    }

    public BookmarkInContextInfo invoke() {
      myBookmarkAtPlace = null;
      myFile = null;
      myLine = -1;

      BookmarkManager bookmarkManager = BookmarkManager.getInstance(myProject);
      Editor editor = CommonDataKeys.EDITOR.getData(myDataContext);
      if (editor != null) {
        if (ToolWindowManager.getInstance(myProject).isEditorComponentActive()) {
          Document document = editor.getDocument();
          myLine = editor.getCaretModel().getLogicalPosition().line;
          myFile = FileDocumentManager.getInstance().getFile(document);
          myBookmarkAtPlace = bookmarkManager.findEditorBookmark(document, myLine);
        }
        else {
          myFile = CommonDataKeys.VIRTUAL_FILE.getData(myDataContext);
          if (myFile != null) {
            Document document = editor.getDocument();
            if (Comparing.equal(myFile, FileDocumentManager.getInstance().getFile(document))) {
              myLine = editor.getCaretModel().getLogicalPosition().line;
              myBookmarkAtPlace = bookmarkManager.findEditorBookmark(document, myLine);
            }
          }
        }
      }

      if (myFile == null) {
        myFile = CommonDataKeys.VIRTUAL_FILE.getData(myDataContext);
        myLine = -1;

        if (myBookmarkAtPlace == null && myFile != null) {
          myBookmarkAtPlace = bookmarkManager.findFileBookmark(myFile);
        }
      }
      return this;
    }
  }

  static List<Bookmark> getSelectedBookmarks(JList<? extends BookmarkItem> list) {
    List<Bookmark> answer = new ArrayList<>();

    for (BookmarkItem value : list.getSelectedValuesList()) {
      if (value != null) {
        answer.add(value.getBookmark());
      }
      else {
        return Collections.emptyList();
      }
    }

    return answer;
  }

  static boolean notFiltered(JList<BookmarkItem> list) {
    ListModel<BookmarkItem> model = list.getModel();
    return !(model instanceof FilteringListModel) ||
           ((FilteringListModel)model).getOriginalModel().getSize() == model.getSize();
  }

  private static class MyDetailView extends DetailViewImpl {
    MyDetailView(Project project) {
      super(project);
    }

    @NotNull
    @Override
    protected Editor createEditor(@Nullable Project project, Document document, VirtualFile file) {
      Editor editor = super.createEditor(project, document, file);
      editor.setBorder(JBUI.Borders.empty());
      return editor;
    }
  }
}