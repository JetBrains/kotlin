// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes.impl;

import com.intellij.CommonBundle;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.templateLanguages.TemplateDataLanguagePatterns;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBDimension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

import static com.intellij.openapi.util.Pair.pair;

/**
 * @author Eugene Belyaev
 */
public class FileTypeConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private RecognizedFileTypes myRecognizedFileType;
  private PatternsPanel myPatterns;
  private FileTypePanel myFileTypePanel;
  private Set<FileType> myTempFileTypes;
  private final FileTypeManagerImpl myManager;
  private FileTypeAssocTable<FileType> myTempPatternsTable;
  private FileTypeAssocTable<Language> myTempTemplateDataLanguages;
  private final Map<UserFileType, UserFileType> myOriginalToEditedMap = new HashMap<>();

  public FileTypeConfigurable(FileTypeManager fileTypeManager) {
    myManager = (FileTypeManagerImpl)fileTypeManager;
  }

  @Override
  public String getDisplayName() {
    return FileTypesBundle.message("filetype.settings.title");
  }

  @Override
  public JComponent createComponent() {
    myFileTypePanel = new FileTypePanel();
    myRecognizedFileType = myFileTypePanel.myRecognizedFileType;
    myPatterns = myFileTypePanel.myPatterns;
    myRecognizedFileType.attachActions(this);
    myRecognizedFileType.myFileTypesList.addListSelectionListener(e -> updateExtensionList());
    myPatterns.attachActions(this);
    myFileTypePanel.myIgnoreFilesField.setColumns(30);
    return myFileTypePanel.getComponent();
  }

  private void updateFileTypeList() {
    List<FileType> types = ContainerUtil.filter(myTempFileTypes, fileType -> !fileType.isReadOnly());
    types.sort((o1, o2) -> o1.getDescription().compareToIgnoreCase(o2.getDescription()));
    myRecognizedFileType.setFileTypes(types.toArray(FileType.EMPTY_ARRAY));
  }

  @NotNull
  private static Set<FileType> getRegisteredFilesTypes() {
    return new HashSet<>(Arrays.asList(FileTypeManager.getInstance().getRegisteredFileTypes()));
  }

  @Override
  public void apply() {
    Set<UserFileType> modifiedUserTypes = myOriginalToEditedMap.keySet();
    for (UserFileType oldType : modifiedUserTypes) {
      oldType.copyFrom(myOriginalToEditedMap.get(oldType));
    }
    myOriginalToEditedMap.clear();

    ApplicationManager.getApplication().runWriteAction(() -> {
      if (!myManager.isIgnoredFilesListEqualToCurrent(myFileTypePanel.myIgnoreFilesField.getText())) {
        myManager.setIgnoredFilesList(myFileTypePanel.myIgnoreFilesField.getText());
      }
      myManager.setPatternsTable(myTempFileTypes, myTempPatternsTable);
      TemplateDataLanguagePatterns.getInstance().setAssocTable(myTempTemplateDataLanguages);
    });
  }

  @Override
  public void reset() {
    myTempPatternsTable = myManager.getExtensionMap().copy();
    myTempTemplateDataLanguages = TemplateDataLanguagePatterns.getInstance().getAssocTable();

    myTempFileTypes = getRegisteredFilesTypes();
    myOriginalToEditedMap.clear();

    updateFileTypeList();
    updateExtensionList();

    myFileTypePanel.myIgnoreFilesField.setText(myManager.getIgnoredFilesList());
  }

  @Override
  public boolean isModified() {
    if (!myManager.isIgnoredFilesListEqualToCurrent(myFileTypePanel.myIgnoreFilesField.getText())) return true;
    return !myTempPatternsTable.equals(myManager.getExtensionMap()) ||
           !myTempFileTypes.equals(getRegisteredFilesTypes()) ||
           !myOriginalToEditedMap.isEmpty() ||
           !myTempTemplateDataLanguages.equals(TemplateDataLanguagePatterns.getInstance().getAssocTable());
  }

  @Override
  public void disposeUIResources() {
    if (myFileTypePanel != null) myFileTypePanel.dispose();
    myFileTypePanel = null;
    myRecognizedFileType = null;
    myPatterns = null;
  }

  private static class ExtensionRenderer extends DefaultListCellRenderer {
    @NotNull
    @Override
    public Component getListCellRendererComponent(@NotNull JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      setText(" " + getText());
      return this;
    }

    @Override
    public Dimension getPreferredSize() {
      return new JBDimension(0, 20);
    }
  }

  private void updateExtensionList() {
    FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;
    List<String> extensions = new ArrayList<>();

    for (FileNameMatcher assoc : myTempPatternsTable.getAssociations(type)) {
      extensions.add(assoc.getPresentableString());
    }

    myPatterns.clearList();
    Collections.sort(extensions);
    for (String extension : extensions) {
      myPatterns.addPattern(extension);
    }
    myPatterns.ensureSelectionExists();
  }

  private void editFileType() {
    FileType fileType = myRecognizedFileType.getSelectedFileType();
    if (!canBeModified(fileType)) return;

    UserFileType ftToEdit = myOriginalToEditedMap.get((UserFileType)fileType);
    if (ftToEdit == null) ftToEdit = ((UserFileType)fileType).clone();
    @SuppressWarnings("unchecked") TypeEditor editor = new TypeEditor(myRecognizedFileType.myFileTypesList, ftToEdit, FileTypesBundle.message("filetype.edit.existing.title"));
    if (editor.showAndGet()) {
      myOriginalToEditedMap.put((UserFileType)fileType, ftToEdit);
    }
  }

  private void removeFileType() {
    FileType fileType = myRecognizedFileType.getSelectedFileType();
    if (fileType == null) return;

    myTempFileTypes.remove(fileType);
    if (fileType instanceof UserFileType) {
      myOriginalToEditedMap.remove((UserFileType)fileType);
    }
    myTempPatternsTable.removeAllAssociations(fileType);

    updateFileTypeList();
    updateExtensionList();
  }

  private static boolean canBeModified(FileType fileType) {
    return fileType instanceof AbstractFileType; //todo: add API for canBeModified
  }

  private void addFileType() {
    //TODO: support adding binary file types...
    AbstractFileType type = new AbstractFileType(new SyntaxTable());
    TypeEditor<AbstractFileType> editor = new TypeEditor<>(myRecognizedFileType.myFileTypesList, type, FileTypesBundle.message("filetype.edit.new.title"));
    if (editor.showAndGet()) {
      myTempFileTypes.add(type);
      updateFileTypeList();
      updateExtensionList();
      myRecognizedFileType.selectFileType(type);
    }
  }

  private void editPattern() {
    final String item = myPatterns.getSelectedItem();
    if (item == null) return;

    editPattern(item);
  }

  private void editPattern(@Nullable final String item) {
    final FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;

    final String title =
      item == null
      ? FileTypesBundle.message("filetype.edit.add.pattern.title")
      : FileTypesBundle.message("filetype.edit.edit.pattern.title");

    final Language oldLanguage = item == null ? null : myTempTemplateDataLanguages.findAssociatedFileType(item);
    final FileTypePatternDialog dialog = new FileTypePatternDialog(item, type, oldLanguage);
    final DialogBuilder builder = new DialogBuilder(myPatterns);
    builder.setPreferredFocusComponent(dialog.getPatternField());
    builder.setCenterPanel(dialog.getMainPanel());
    builder.setTitle(title);
    builder.showModal(true);
    if (builder.getDialogWrapper().isOK()) {
      final String pattern = dialog.getPatternField().getText();
      if (StringUtil.isEmpty(pattern)) return;

      final FileNameMatcher matcher = FileTypeManager.parseFromString(pattern);
      FileType registeredFileType = findExistingFileType(matcher);
      if (registeredFileType != null && registeredFileType != type) {
        if (registeredFileType.isReadOnly()) {
          Messages.showMessageDialog(myPatterns.myPatternsList,
                                     FileTypesBundle.message("filetype.edit.add.pattern.exists.error", registeredFileType.getDescription()),
                                     title, Messages.getErrorIcon());
          return;
        }
        else {
          if (Messages.OK == Messages.showOkCancelDialog(myPatterns.myPatternsList, FileTypesBundle.message("filetype.edit.add.pattern.exists.message",
                                                                                               registeredFileType.getDescription()),
                                               FileTypesBundle.message("filetype.edit.add.pattern.exists.title"),
                                               FileTypesBundle.message("filetype.edit.add.pattern.reassign.button"),
                                               CommonBundle.getCancelButtonText(), Messages.getQuestionIcon())) {
            myTempPatternsTable.removeAssociation(matcher, registeredFileType);
            if (oldLanguage != null) {
              myTempTemplateDataLanguages.removeAssociation(matcher, oldLanguage);
            }
          }
          else {
            return;
          }
        }
      }

      if (item != null) {
        final FileNameMatcher oldMatcher = FileTypeManager.parseFromString(item);
        myTempPatternsTable.removeAssociation(oldMatcher, type);
        if (oldLanguage != null) {
          myTempTemplateDataLanguages.removeAssociation(oldMatcher, oldLanguage);
        }
      }
      myTempPatternsTable.addAssociation(matcher, type);
      Language language = dialog.getTemplateDataLanguage();
      if (language != null) myTempTemplateDataLanguages.addAssociation(matcher, language);

      updateExtensionList();
      final int index = myPatterns.getListModel().indexOf(matcher.getPresentableString());
      if (index >= 0) {
        ScrollingUtil.selectItem(myPatterns.myPatternsList, index);
      }
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myPatterns.myPatternsList, true));
    }
  }

  private void addPattern() {
    editPattern(null);
  }

  @Nullable
  public FileType findExistingFileType(FileNameMatcher matcher) {
    FileType fileTypeByExtension = myTempPatternsTable.findAssociatedFileType(matcher);

    if (fileTypeByExtension != null && fileTypeByExtension != FileTypes.UNKNOWN) {
      return fileTypeByExtension;
    }
    FileType registeredFileType = FileTypeManager.getInstance().getFileTypeByExtension(matcher.getPresentableString());
    if (registeredFileType != FileTypes.UNKNOWN && registeredFileType.isReadOnly()) {
      return registeredFileType;
    }
    return null;
  }

  private void removePattern() {
    FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;
    String extension = myPatterns.removeSelected();
    if (extension == null) return;
    FileNameMatcher matcher = FileTypeManager.parseFromString(extension);

    myTempPatternsTable.removeAssociation(matcher, type);
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myPatterns.myPatternsList, true));
  }

  @NotNull
  @Override
  public String getHelpTopic() {
    return "preferences.fileTypes";
  }

  public static class RecognizedFileTypes extends JPanel {
    private final JList<FileType> myFileTypesList;
    private final MySpeedSearch mySpeedSearch;
    private FileTypeConfigurable myController;

    public RecognizedFileTypes() {
      super(new BorderLayout());

      myFileTypesList = new JBList<>(new DefaultListModel<>());
      myFileTypesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myFileTypesList.setCellRenderer(new FileTypeRenderer(() -> {
        List<FileType> result = new ArrayList<>();
        for (int i = 0; i < myFileTypesList.getModel().getSize(); i++) {
          result.add(myFileTypesList.getModel().getElementAt(i));
        }
        return result;
      }));

      new DoubleClickListener() {
        @Override
        protected boolean onDoubleClick(MouseEvent e) {
          myController.editFileType();
          return true;
        }
      }.installOn(myFileTypesList);

      ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(myFileTypesList)
        .setAddAction(button -> myController.addFileType())
        .setRemoveAction(button -> myController.removeFileType())
        .setEditAction(button -> myController.editFileType())
        .setEditActionUpdater(e -> {
          final FileType fileType = getSelectedFileType();
          return canBeModified(fileType);
        })
        .setRemoveActionUpdater(e -> canBeModified(getSelectedFileType()))
        .disableUpDownActions();

      add(toolbarDecorator.createPanel(), BorderLayout.CENTER);
      setBorder(IdeBorderFactory.createTitledBorder(FileTypesBundle.message("filetype.recognized.group"), false));

      mySpeedSearch = new MySpeedSearch(myFileTypesList);
    }

    private static class MySpeedSearch extends SpeedSearchBase<JList> {
      private final List<Condition<Pair<Object, String>>> myOrderedConverters;
      private FileTypeConfigurable myController;
      private Object myCurrentType;
      private String myExtension;

      private MySpeedSearch(JList component) {
        super(component);
        myOrderedConverters = Arrays.asList(
          // simple
          p -> {
            String value = p.first.toString();
            if (p.first instanceof FileType) {
              value = ((FileType)p.first).getDescription();
            }
            return getComparator().matchingFragments(p.second, value) != null;
          },
          // by-extension
          p -> (p.first instanceof FileType && myCurrentType != null) && myCurrentType.equals(p.first)
        );
      }

      @Override
      protected boolean isMatchingElement(Object element, String pattern) {
        for (Condition<Pair<Object, String>> convertor : myOrderedConverters) {
          boolean matched = convertor.value(pair(element, pattern));
          if (matched) return true;
        }
        return false;
      }

      @Nullable
      @Override
      protected final String getElementText(Object element) {
        throw new IllegalStateException();
      }

      @Override
      protected int getSelectedIndex() {
        return myComponent.getSelectedIndex();
      }

      @NotNull
      @Override
      protected Object[] getAllElements() {
        return ListSpeedSearch.getAllListElements(myComponent);
      }

      @Override
      protected void selectElement(Object element, String selectedText) {
        ScrollingUtil.selectItem(myComponent, element);
        if (myCurrentType != null && myCurrentType.equals(element) && myController != null) {
          myController.myPatterns.select(myExtension);
        }
      }

      @Override
      protected void onSearchFieldUpdated(String s) {
        if (myController == null || myController.myTempPatternsTable == null) return;
        int index = s.lastIndexOf('.');
        if (index < 0) {
          s = "." + s;
        }
        myCurrentType = myController.myTempPatternsTable.findAssociatedFileType(s);
        if (myCurrentType != null) {
          myExtension = s;
        } else {
          myExtension = null;
        }
      }
    }

    public void attachActions(final FileTypeConfigurable controller) {
      myController = controller;
      mySpeedSearch.myController = controller;
    }

    public FileType getSelectedFileType() {
      return myFileTypesList.getSelectedValue();
    }

    public JComponent getComponent() {
      return this;
    }

    public void setFileTypes(FileType[] types) {
      DefaultListModel<FileType> listModel = (DefaultListModel<FileType>)myFileTypesList.getModel();
      listModel.clear();
      for (FileType type : types) {
        if (type != FileTypes.UNKNOWN) {
          listModel.addElement(type);
        }
      }
      ScrollingUtil.ensureSelectionExists(myFileTypesList);
    }

    public int getSelectedIndex() {
      return myFileTypesList.getSelectedIndex();
    }

    public void selectFileType(FileType fileType) {
      myFileTypesList.setSelectedValue(fileType, true);
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myFileTypesList, true));
    }
  }

  public static class PatternsPanel extends JPanel {
    private final JBList<String> myPatternsList;
    private FileTypeConfigurable myController;

    public PatternsPanel() {
      super(new BorderLayout());
      myPatternsList = new JBList<>(new DefaultListModel<>());
      myPatternsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myPatternsList.setCellRenderer(new ExtensionRenderer());
      myPatternsList.getEmptyText().setText(FileTypesBundle.message("filetype.settings.no.patterns"));

      add(ToolbarDecorator.createDecorator(myPatternsList)
            .setAddAction(button -> myController.addPattern()).setEditAction(button -> myController.editPattern()).setRemoveAction(
          button -> myController.removePattern()).disableUpDownActions().createPanel(), BorderLayout.CENTER);

      setBorder(IdeBorderFactory.createTitledBorder(FileTypesBundle.message("filetype.registered.patterns.group"), false));
    }

    public void attachActions(final FileTypeConfigurable controller) {
      myController = controller;
    }

    public JComponent getComponent() {
      return this;
    }

    public void clearList() {
      getListModel().clear();
      myPatternsList.clearSelection();
    }

    private DefaultListModel<String> getListModel() {
      return (DefaultListModel<String>)myPatternsList.getModel();
    }

    public void addPattern(String pattern) {
      getListModel().addElement(pattern);
    }

    public void ensureSelectionExists() {
      ScrollingUtil.ensureSelectionExists(myPatternsList);
    }

    public void select(final String pattern) {
      for (int i = 0; i < myPatternsList.getItemsCount(); i++) {
        final String at = myPatternsList.getModel().getElementAt(i);
        final FileNameMatcher matcher = FileTypeManager.parseFromString(at);
        if (matcher.acceptsCharSequence(pattern)) {
          ScrollingUtil.selectItem(myPatternsList, i);
          return;
        }
      }
    }

    public String removeSelected() {
      String selectedValue = myPatternsList.getSelectedValue();
      if (selectedValue == null) return null;
      ListUtil.removeSelectedItems(myPatternsList);
      return selectedValue;
    }

    public String getDefaultExtension() {
      return getListModel().getElementAt(0);
    }

    public String getSelectedItem() {
      return myPatternsList.getSelectedValue();
    }
  }

  private static class FileTypePanel {
    private JPanel myWholePanel;
    private RecognizedFileTypes myRecognizedFileType;
    private PatternsPanel myPatterns;
    private JTextField myIgnoreFilesField;

    public JComponent getComponent() {
      return myWholePanel;
    }

    public void dispose() {
      myRecognizedFileType.setFileTypes(FileType.EMPTY_ARRAY);
      myPatterns.clearList();
    }
  }

  private static class TypeEditor<T extends UserFileType<T>> extends DialogWrapper {
    private final T myFileType;
    private final SettingsEditor<T> myEditor;

    TypeEditor(Component parent, T fileType, final String title) {
      super(parent, false);
      myFileType = fileType;
      myEditor = fileType.getEditor();
      setTitle(title);
      init();
      Disposer.register(myDisposable, myEditor);
    }

    @Override
    protected void init() {
      super.init();
      myEditor.resetFrom(myFileType);
    }

    @Override
    protected JComponent createCenterPanel() {
      return myEditor.getComponent();
    }

    @Override
    protected void doOKAction() {
      try {
        myEditor.applyTo(myFileType);
      }
      catch (ConfigurationException e) {
        Messages.showErrorDialog(getContentPane(), e.getMessage(), e.getTitle());
        return;
      }
      super.doOKAction();
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    protected String getHelpId() {
      return "reference.dialogs.newfiletype";
    }
  }

  @NotNull
  @Override
  public String getId() {
    return getHelpTopic();
  }
}