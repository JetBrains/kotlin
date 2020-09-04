// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes.impl;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.ide.lightEdit.LightEditFilePatterns;
import com.intellij.ide.lightEdit.LightEditService;
import com.intellij.lang.LangBundle;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.templateLanguages.TemplateDataLanguagePatterns;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

import static com.intellij.openapi.util.Pair.pair;

public final class FileTypeConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private static final Insets TITLE_INSETS = JBUI.insetsTop(8);

  private RecognizedFileTypes myRecognizedFileType;
  private PatternsPanel myPatterns;
  private HashBangPanel myHashBangs;
  private FileTypePanel myFileTypePanel;
  private Set<FileType> myTempFileTypes;
  private FileTypeAssocTable<FileType> myTempPatternsTable;
  private FileTypeAssocTable<Language> myTempTemplateDataLanguages;
  private final Map<UserFileType, UserFileType> myOriginalToEditedMap = new HashMap<>();

  @Override
  public String getDisplayName() {
    return FileTypesBundle.message("filetype.settings.title");
  }

  @Override
  public JComponent createComponent() {
    myFileTypePanel = new FileTypePanel();
    myFileTypePanel.myIgnorePanel.setBorder(
      IdeBorderFactory.createTitledBorder(IdeBundle.message("editbox.ignore.files.and.folders"), false, TITLE_INSETS).setShowLine(false));
    myRecognizedFileType = new RecognizedFileTypes(myFileTypePanel.myRecognizedFileTypesPanel);
    myPatterns = new PatternsPanel(myFileTypePanel.myPatternsPanel);
    myHashBangs = new HashBangPanel(myFileTypePanel.myHashBangPanel);
    myRecognizedFileType.myFileTypesList.addListSelectionListener(__ -> updateExtensionList());
    myFileTypePanel.myIgnoreFilesField.setColumns(30);
    //noinspection DialogTitleCapitalization - it's an option label
    myFileTypePanel.myOpenWithLightEditPanel.setBorder(
      IdeBorderFactory.createTitledBorder(IdeBundle.message("editbox.open.in.light.edit.mode"), false, TITLE_INSETS).setShowLine(false));
    myFileTypePanel.myLightEditHintLabel.setForeground(JBColor.GRAY);
    myFileTypePanel.myLightEditHintLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    return myFileTypePanel.myWholePanel;
  }

  private void updateFileTypeList() {
    List<FileType> types = ContainerUtil.filter(myTempFileTypes, fileType -> !fileType.isReadOnly());
    types.sort((o1, o2) -> o1.getDescription().compareToIgnoreCase(o2.getDescription()));
    myRecognizedFileType.setFileTypes(types.toArray(FileType.EMPTY_ARRAY));
  }

  @NotNull
  private static Set<FileType> getRegisteredFilesTypes() {
    return ContainerUtil.set(FileTypeManager.getInstance().getRegisteredFileTypes());
  }

  @Override
  public void apply() {
    Set<UserFileType> modifiedUserTypes = myOriginalToEditedMap.keySet();
    for (UserFileType oldType : modifiedUserTypes) {
      oldType.copyFrom(myOriginalToEditedMap.get(oldType));
    }
    myOriginalToEditedMap.clear();

    FileTypeManagerImpl fileTypeManager = (FileTypeManagerImpl)FileTypeManager.getInstance();
    ApplicationManager.getApplication().runWriteAction(() -> {
      if (!fileTypeManager.isIgnoredFilesListEqualToCurrent(myFileTypePanel.myIgnoreFilesField.getText())) {
        fileTypeManager.setIgnoredFilesList(myFileTypePanel.myIgnoreFilesField.getText());
      }
      fileTypeManager.setPatternsTable(myTempFileTypes, myTempPatternsTable);
      TemplateDataLanguagePatterns.getInstance().setAssocTable(myTempTemplateDataLanguages);
    });

    LightEditService.getInstance().setSupportedFilePatterns(
      LightEditFilePatterns.parse(myFileTypePanel.myLightEditPatternsField.getText()));
  }

  @Override
  public void reset() {
    FileTypeManagerImpl fileTypeManager = (FileTypeManagerImpl)FileTypeManager.getInstance();
    myTempPatternsTable = fileTypeManager.getExtensionMap().copy();
    myTempTemplateDataLanguages = TemplateDataLanguagePatterns.getInstance().getAssocTable();

    myTempFileTypes = getRegisteredFilesTypes();
    myOriginalToEditedMap.clear();

    updateFileTypeList();
    updateExtensionList();

    myFileTypePanel.myIgnoreFilesField.setText(fileTypeManager.getIgnoredFilesList());

    myFileTypePanel.myLightEditPatternsField.setText(
      LightEditService.getInstance().getSupportedFilePatterns().toSeparatedString());
  }

  @Override
  public boolean isModified() {
    FileTypeManagerImpl fileTypeManager = (FileTypeManagerImpl)FileTypeManager.getInstance();
    if (!fileTypeManager.isIgnoredFilesListEqualToCurrent(myFileTypePanel.myIgnoreFilesField.getText())) {
      return true;
    }
    return !myTempPatternsTable.equals(fileTypeManager.getExtensionMap()) ||
           !myTempFileTypes.equals(getRegisteredFilesTypes()) ||
           !myOriginalToEditedMap.isEmpty() ||
           !myTempTemplateDataLanguages.equals(TemplateDataLanguagePatterns.getInstance().getAssocTable()) ||
           !LightEditFilePatterns.parse(myFileTypePanel.myLightEditPatternsField.getText())
             .equals(LightEditService.getInstance().getSupportedFilePatterns());
  }

  @Override
  public void disposeUIResources() {
    if (myFileTypePanel != null) {
      myRecognizedFileType.setFileTypes(FileType.EMPTY_ARRAY);
      myPatterns.clearList();
      myHashBangs.clearList();
    }
    myFileTypePanel = null;
    myRecognizedFileType = null;
    myPatterns = null;
    myHashBangs = null;
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

    myPatterns.refill(extensions);
    myHashBangs.refill(myTempPatternsTable.getHashBangPatterns(type));
  }

  private void editFileType() {
    FileType fileType = myRecognizedFileType.getSelectedFileType();
    if (!canBeModified(fileType)) return;

    UserFileType userFileType = (UserFileType)fileType;
    UserFileType ftToEdit = myOriginalToEditedMap.get(userFileType);
    if (ftToEdit == null) ftToEdit = userFileType.clone();
    @SuppressWarnings("unchecked") TypeEditor editor = new TypeEditor(myRecognizedFileType.myFileTypesList, ftToEdit, FileTypesBundle.message("filetype.edit.existing.title"));
    if (editor.showAndGet()) {
      myOriginalToEditedMap.put(userFileType, ftToEdit);
    }
  }

  private void removeFileType() {
    FileType fileType = myRecognizedFileType.getSelectedFileType();
    if (fileType == null) return;

    myTempFileTypes.remove(fileType);
    if (fileType instanceof UserFileType) {
      myOriginalToEditedMap.remove(fileType);
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
    String item = myPatterns.getSelectedItem();
    if (item == null) return;

    editPattern(item);
  }

  private void editPattern(@Nullable String item) {
    FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;

    String title =
      item == null
      ? FileTypesBundle.message("filetype.edit.add.pattern.title")
      : FileTypesBundle.message("filetype.edit.edit.pattern.title");

    Language oldLanguage = item == null ? null : myTempTemplateDataLanguages.findAssociatedFileType(item);
    FileTypePatternDialog dialog = new FileTypePatternDialog(item, type, oldLanguage);
    DialogBuilder builder = new DialogBuilder(myPatterns.myList);
    builder.setPreferredFocusComponent(dialog.getPatternField());
    builder.setCenterPanel(dialog.getMainPanel());
    builder.setTitle(title);
    builder.showModal(true);
    if (builder.getDialogWrapper().isOK()) {
      String pattern = dialog.getPatternField().getText();
      if (StringUtil.isEmpty(pattern)) return;

      FileNameMatcher matcher = FileTypeManager.parseFromString(pattern);
      FileType registeredFileType = findExistingFileType(matcher);
      if (registeredFileType != null && registeredFileType != type) {
        if (registeredFileType.isReadOnly()) {
          Messages.showMessageDialog(myPatterns.myList,
                                     FileTypesBundle.message("filetype.edit.add.pattern.exists.error", registeredFileType.getDescription()),
                                     title, Messages.getErrorIcon());
          return;
        }
        int ret = Messages.showOkCancelDialog(myPatterns.myList, FileTypesBundle.message("filetype.edit.add.pattern.exists.message",
                                                                                         registeredFileType.getDescription()),
                                              FileTypesBundle.message("filetype.edit.add.pattern.exists.title"),
                                              FileTypesBundle.message("filetype.edit.add.pattern.reassign.button"),
                                              CommonBundle.getCancelButtonText(), Messages.getQuestionIcon());
        if (ret == Messages.OK) {
          myTempPatternsTable.removeAssociation(matcher, registeredFileType);
          if (oldLanguage != null) {
            myTempTemplateDataLanguages.removeAssociation(matcher, oldLanguage);
          }
        }
        else {
          return;
        }
      }

      if (item != null) {
        FileNameMatcher oldMatcher = FileTypeManager.parseFromString(item);
        myTempPatternsTable.removeAssociation(oldMatcher, type);
        if (oldLanguage != null) {
          myTempTemplateDataLanguages.removeAssociation(oldMatcher, oldLanguage);
        }
      }
      myTempPatternsTable.addAssociation(matcher, type);
      Language language = dialog.getTemplateDataLanguage();
      if (language != null) {
        myTempTemplateDataLanguages.addAssociation(matcher, language);
      }

      updateExtensionList();
      myPatterns.select(pattern);
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myPatterns.myList, true));
    }
  }

  private void addPattern() {
    editPattern(null);
  }

  @Nullable
  private FileType findExistingFileType(@NotNull FileNameMatcher matcher) {
    FileType type = myTempPatternsTable.findAssociatedFileType(matcher);

    if (type != null && type != FileTypes.UNKNOWN) {
      return type;
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
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myPatterns.myList, true));
  }
  private void removeHashBang() {
    FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;
    String extension = myHashBangs.removeSelected();
    if (extension == null) return;

    myTempPatternsTable.removeHashBangPattern(extension, type);
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myHashBangs.myList, true));
  }

  @NotNull
  @Override
  public String getHelpTopic() {
    return "preferences.fileTypes";
  }

  class RecognizedFileTypes {
    private final JList<FileType> myFileTypesList = new JBList<>(new DefaultListModel<>());
    RecognizedFileTypes(@NotNull JPanel panel) {
      panel.setLayout(new BorderLayout());

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
        protected boolean onDoubleClick(@NotNull MouseEvent e) {
          editFileType();
          return true;
        }
      }.installOn(myFileTypesList);

      ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(myFileTypesList)
        .setAddAction(__ -> addFileType())
        .setRemoveAction(__ -> removeFileType())
        .setEditAction(__ -> editFileType())
        .setEditActionUpdater(e -> {
          FileType fileType = getSelectedFileType();
          return canBeModified(fileType);
        })
        .setRemoveActionUpdater(e -> canBeModified(getSelectedFileType()))
        .disableUpDownActions();

      panel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER);
      panel.setBorder(IdeBorderFactory.createTitledBorder(FileTypesBundle.message("filetype.recognized.group"), false, TITLE_INSETS).setShowLine(false));

      new MySpeedSearch(myFileTypesList);
    }

    private class MySpeedSearch extends SpeedSearchBase<JList<FileType>> {
      private final List<Condition<Pair<Object, String>>> myOrderedConverters;
      private Object myCurrentType;
      private String myExtension;

      private MySpeedSearch(@NotNull JList<FileType> component) {
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
          p -> p.first instanceof FileType && myCurrentType != null && myCurrentType.equals(p.first)
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

      @Override
      protected Object @NotNull [] getAllElements() {
        return ListSpeedSearch.getAllListElements(myComponent);
      }

      @Override
      protected void selectElement(Object element, String selectedText) {
        if (element != null) {
          ScrollingUtil.selectItem(myComponent, (FileType)element);
          if (element.equals(myCurrentType)) {
            myPatterns.select(myExtension);
          }
        }
      }

      @Override
      protected void onSearchFieldUpdated(String s) {
        if (myTempPatternsTable == null) return;
        int index = s.lastIndexOf('.');
        if (index < 0) {
          s = "." + s;
        }
        myCurrentType = myTempPatternsTable.findAssociatedFileType(s);
        if (myCurrentType != null) {
          myExtension = s;
        }
        else {
          myExtension = null;
        }
      }
    }

    FileType getSelectedFileType() {
      return myFileTypesList.getSelectedValue();
    }

    void setFileTypes(FileType @NotNull [] types) {
      DefaultListModel<FileType> listModel = (DefaultListModel<FileType>)myFileTypesList.getModel();
      listModel.clear();
      for (FileType type : types) {
        if (type != FileTypes.UNKNOWN) {
          listModel.addElement(type);
        }
      }
      ScrollingUtil.ensureSelectionExists(myFileTypesList);
    }

    void selectFileType(@NotNull FileType fileType) {
      myFileTypesList.setSelectedValue(fileType, true);
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myFileTypesList, true));
    }
  }

  class PatternsPanel {
    private final JBList<String> myList = new JBList<>(new DefaultListModel<>());

    PatternsPanel(@NotNull JPanel panel) {
      panel.setLayout(new BorderLayout());
      myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myList.setCellRenderer(new ExtensionRenderer());
      myList.getEmptyText().setText(FileTypesBundle.message("filetype.settings.no.patterns"));

      ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myList)
        .setAddAction(__ -> addPattern())
        .setEditAction(__ -> editPattern())
        .setRemoveAction(__ -> removePattern())
        .disableUpDownActions();
      panel.add(decorator.createPanel(), BorderLayout.CENTER);

      panel.setBorder(IdeBorderFactory.createTitledBorder(FileTypesBundle.message("filetype.registered.patterns.group"), false, TITLE_INSETS).setShowLine(false));
    }

    void clearList() {
      getListModel().clear();
      myList.clearSelection();
    }

    @NotNull
    private DefaultListModel<String> getListModel() {
      return (DefaultListModel<String>)myList.getModel();
    }

    void select(@NotNull String pattern) {
      for (int i = 0; i < myList.getItemsCount(); i++) {
        String at = myList.getModel().getElementAt(i);
        FileNameMatcher matcher = FileTypeManager.parseFromString(at);
        if (matcher.acceptsCharSequence(pattern)) {
          ScrollingUtil.selectItem(myList, i);
          return;
        }
      }
    }

    String removeSelected() {
      String selectedValue = getSelectedItem();
      if (selectedValue == null) return null;
      ListUtil.removeSelectedItems(myList);
      return selectedValue;
    }

    String getSelectedItem() {
      return myList.getSelectedValue();
    }

    private void refill(@NotNull List<String> extensions) {
      clearList();
      Collections.sort(extensions);
      for (String extension : extensions) {
        getListModel().addElement(extension);
      }
      ScrollingUtil.ensureSelectionExists(myList);
    }
  }

  class HashBangPanel {
    private final JBList<String> myList = new JBList<>(new DefaultListModel<>());

    HashBangPanel(@NotNull JPanel panel) {
      panel.setLayout(new BorderLayout());
      myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      myList.setCellRenderer(new ExtensionRenderer(){
        @Override
        public @NotNull Component getListCellRendererComponent(@NotNull JList list,
                                                               Object value,
                                                               int index,
                                                               boolean isSelected,
                                                               boolean cellHasFocus) {
          Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          setText(" #!*"+value+"*");
          return component;
        }
      });
      myList.getEmptyText().setText(FileTypesBundle.message("filetype.settings.no.patterns"));

      ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myList)
        .setAddAction(__ -> editHashBang(null))
        .setAddActionName(LangBundle.message("action.HashBangPanel.add.hashbang.pattern.text"))
        .setEditAction(__ -> editHashBang())
        .setRemoveAction(__ -> removeHashBang())
        .disableUpDownActions();

      panel.add(decorator.createPanel(), BorderLayout.CENTER);

      panel.setBorder(IdeBorderFactory.createTitledBorder(FileTypesBundle.message("filetype.hashbang.group"), false, TITLE_INSETS).setShowLine(false));
    }

    void clearList() {
      getListModel().clear();
      myList.clearSelection();
    }

    @NotNull
    private DefaultListModel<String> getListModel() {
      return (DefaultListModel<String>)myList.getModel();
    }

    void select(@NotNull String pattern) {
      ScrollingUtil.selectItem(myList, pattern);
    }

    String removeSelected() {
      String selectedValue = getSelectedItem();
      if (selectedValue == null) return null;
      ListUtil.removeSelectedItems(myList);
      return selectedValue;
    }

    String getSelectedItem() {
      return myList.getSelectedValue();
    }

    private void refill(@NotNull List<String> values) {
      clearList();
      Collections.sort(values);
      for (String extension : values) {
        getListModel().addElement(extension);
      }
      ScrollingUtil.ensureSelectionExists(myList);
    }
  }

  private void editHashBang() {
    String item = myHashBangs.getSelectedItem();
    if (item == null) return;

    editHashBang(item);
  }
  private void editHashBang(@Nullable("null means new") String oldHashBang) {
    FileType type = myRecognizedFileType.getSelectedFileType();
    if (type == null) return;

    String title = FileTypesBundle.message("filetype.edit.hashbang.title");

    Language oldLanguage = oldHashBang == null ? null : myTempTemplateDataLanguages.findAssociatedFileType(oldHashBang);
    String hashbang = Messages.showInputDialog(myHashBangs.myList, FileTypesBundle.message("filetype.edit.hashbang.prompt"), title, null, oldHashBang, null);
    if (StringUtil.isEmpty(hashbang)) {
      return; //canceled or empty
    }
    HashBangConflict conflict = checkHashBangConflict(hashbang);
    if (conflict != null) {
      FileType existingFileType = conflict.fileType;
      if (existingFileType == type) return; // ignore duplicate
      if (!conflict.writeable) {
        String message = conflict.exact ? FileTypesBundle.message("filetype.edit.hashbang.exists.exact.error", existingFileType.getDescription())
                         : FileTypesBundle.message("filetype.edit.hashbang.exists.similar.error", existingFileType.getDescription(), conflict.existingHashBang);
        Messages.showMessageDialog(myHashBangs.myList, message, title, Messages.getErrorIcon());
        return;
      }
      String message = conflict.exact ? FileTypesBundle.message("filetype.edit.hashbang.exists.exact.message", existingFileType.getDescription())
                                      : FileTypesBundle.message("filetype.edit.hashbang.exists.similar.message", existingFileType.getDescription(), conflict.existingHashBang);
      int ret = Messages.showOkCancelDialog(myHashBangs.myList, message,
                                            FileTypesBundle.message("filetype.edit.hashbang.exists.title"),
                                            FileTypesBundle.message("filetype.edit.hashbang.reassign.button"),
                                            CommonBundle.getCancelButtonText(), Messages.getQuestionIcon());
      if (ret != Messages.OK) {
        return;
      }
      myTempPatternsTable.removeHashBangPattern(hashbang, existingFileType);
      if (oldLanguage != null) {
        myTempTemplateDataLanguages.removeHashBangPattern(hashbang, oldLanguage);
      }
      myTempPatternsTable.removeHashBangPattern(conflict.existingHashBang, conflict.fileType);
    }
    if (oldHashBang != null) {
      myTempPatternsTable.removeHashBangPattern(oldHashBang, type);
      if (oldLanguage != null) {
        myTempTemplateDataLanguages.removeHashBangPattern(oldHashBang, oldLanguage);
      }
    }
    myTempPatternsTable.addHashBangPattern(hashbang, type);

    updateExtensionList();
    myHashBangs.select(hashbang);
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myPatterns.myList, true));
  }

  // describes conflict between two hashbang patterns when user tried to create new/edit existing hashbang
  private static class HashBangConflict {
    FileType fileType; // conflicting file type
    boolean exact; // true: conflict with the file type with the exactly the same hashbang/false: similar hashbang (more selective or less selective)
    boolean writeable; //file type can be changed
    String existingHashBang; // the hashbang of the conflicting file type
  }
  private static boolean isStandardFileType(@NotNull FileType fileType) {
    return FileTypeManager.getInstance().getStdFileType(fileType.getName()) == fileType;
  }

  // check if there is a conflict between new hasbang and exising ones
  private HashBangConflict checkHashBangConflict(@NotNull String hashbang) {
    HashBangConflict conflict = new HashBangConflict();
    for (Map.Entry<String, FileType> entry : myTempPatternsTable.getAllHashBangPatterns().entrySet()) {
      String existingHashBang = entry.getKey();
      if (hashbang.contains(existingHashBang) || existingHashBang.contains(hashbang)) {
        conflict.fileType = entry.getValue();
        conflict.exact = existingHashBang.equals(hashbang);
        conflict.writeable = !conflict.fileType.isReadOnly() && !isStandardFileType(conflict.fileType);
        conflict.existingHashBang = existingHashBang;
        return conflict;
      }
    }
    List<FileTypeRegistry.FileTypeDetector> detectors = FileTypeRegistry.FileTypeDetector.EP_NAME.getExtensionList();
    for (FileTypeRegistry.FileTypeDetector detector : detectors) {
      if (detector instanceof HashBangFileTypeDetector) {
        String existingHashBang = ((HashBangFileTypeDetector)detector).getMarker();
        if (hashbang.contains(existingHashBang) || existingHashBang.contains(hashbang)) {
          conflict.fileType = ((HashBangFileTypeDetector)detector).getFileType();
          conflict.exact = existingHashBang.equals(hashbang);
          conflict.writeable = false;
          conflict.existingHashBang = existingHashBang;
          return conflict;
        }
      }
    }
    return null;
  }

  @NotNull
  @Override
  public String getId() {
    return getHelpTopic();
  }
}