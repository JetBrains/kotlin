// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors;

import com.intellij.application.options.schemes.AbstractSchemeActions;
import com.intellij.application.options.schemes.AbstractSchemesPanel;
import com.intellij.application.options.schemes.SchemeNameGenerator;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme;
import com.intellij.openapi.editor.colors.impl.DefaultColorsScheme;
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl;
import com.intellij.openapi.editor.colors.impl.EmptyColorScheme;
import com.intellij.openapi.options.*;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.scale.JBUIScale;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class ColorSchemeActions extends AbstractSchemeActions<EditorColorsScheme> {

  protected ColorSchemeActions(@NotNull AbstractSchemesPanel<EditorColorsScheme, ?> schemesPanel) {
    super(schemesPanel);
  }

  @NotNull
  @Override
  protected Collection<String> getSchemeImportersNames() {
    List<String> importersNames = new ArrayList<>();
    for (ImportHandler importHandler : ImportHandler.EP_NAME.getExtensionList()) {
      importersNames.add(importHandler.getTitle());
    }
    importersNames.addAll(super.getSchemeImportersNames());
    return importersNames;
  }

  @Override
  protected void importScheme(@NotNull String importerName) {
    if (tryImportWithImportHandler(importerName)) {
      return;
    }
    final SchemeImporter<EditorColorsScheme> importer = SchemeImporterEP.getImporter(importerName, EditorColorsScheme.class);
    if (importer != null) {
      VirtualFile importSource = importer.getImportFile();
      if (importSource == null) {
        importSource =
          SchemeImportUtil.selectImportSource(importer.getSourceExtensions(), getSchemesPanel(), null, "Choose " + importerName);
      }
      if (importSource != null) {
        if ("jar".equals(importSource.getExtension())) {
          importFromJar(getSchemesPanel().getToolbar(), importer, importSource);
        }
        else {
          doImport(importer, importSource);
        }
      }
    }
  }

  private void doImport(@NotNull SchemeImporter<EditorColorsScheme> importer, @NotNull VirtualFile importSource) {
    try {
      EditorColorsScheme imported =
        importer.importScheme(DefaultProjectFactory.getInstance().getDefaultProject(), importSource, getOptions().getSelectedScheme(),
                              name -> {
                                String newName = SchemeNameGenerator.getUniqueName(name != null ? name : "Unnamed",
                                                                                   candidate -> getSchemesPanel().getModel()
                                                                                     .containsScheme(candidate, false));
                                AbstractColorsScheme newScheme = new EditorColorsSchemeImpl(EmptyColorScheme.INSTANCE);
                                newScheme.setName(newName);
                                newScheme.setDefaultMetaInfo(EmptyColorScheme.INSTANCE);
                                return newScheme;
                              });
      if (imported != null) {
        getOptions().addImportedScheme(imported);
        String message = importer.getAdditionalImportInfo(imported);
        if (message == null) {
          message =
            ApplicationBundle.message("settings.editor.scheme.import.success", importSource.getPresentableUrl(), imported.getName());
        }
        getSchemesPanel().showStatus(message, MessageType.INFO);
      }
    }
    catch (SchemeImportException e) {
      handleError(e, importSource);
    }
  }

  private void handleError(@NotNull SchemeImportException e, @NotNull VirtualFile importSource) {
    String details = e.getMessage();
    getSchemesPanel()
      .showStatus(
        ApplicationBundle.message("settings.editor.scheme.import.failure", importSource.getPresentableUrl()) +
        (StringUtil.isEmpty(details) ? "" : "\n" + details),
        MessageType.ERROR);
  }

  private void importFromJar(@NotNull Component componentAbove,
                             @NotNull SchemeImporter<EditorColorsScheme> importer,
                             @NotNull VirtualFile jarFile) {
    try {
      List<VirtualFile> schemeFiles = getSchemeFiles(jarFile);
      if (schemeFiles.size() == 1 || ApplicationManager.getApplication().isUnitTestMode()) {
        doImport(importer, schemeFiles.iterator().next());
        return;
      }
      List<ColorSchemeItem> fileList = new ArrayList<>(schemeFiles.size());
      for (VirtualFile file : schemeFiles) {
        Element root = SchemeImportUtil.loadSchemeDom(file);
        String name = StringUtil.trimStart(ColorSchemeImporter.getSchemeName(root), SchemeManager.EDITABLE_COPY_PREFIX);
        fileList.add(new ColorSchemeItem(name, file));
      }
      ImportSchemeChooserDialog dialog = new ImportSchemeChooserDialog(mySchemesPanel, componentAbove, fileList);
      if (dialog.showAndGet()) {
        List<ColorSchemeItem> selectedItems = dialog.getSelectedItems();
        for (ColorSchemeItem item : selectedItems) {
          doImport(importer, item.getFile());
        }
      }
    }
    catch (SchemeImportException e) {
      handleError(e, jarFile);
    }
  }

  private static List<VirtualFile> getSchemeFiles(@NotNull VirtualFile jarFile) throws SchemeImportException {
    List<VirtualFile> schemeFiles = new ArrayList<>();
    for (VirtualFile file : jarFile.getChildren()) {
      if (file.isDirectory() && "colors".equals(file.getName())) {
        for (VirtualFile schemeFile : file.getChildren()) {
          String ext = schemeFile.getExtension();
          if ("icls".equals(ext) || "xml".equals(ext)) {
            schemeFiles.add(schemeFile);
          }
        }
        break;
      }
    }
    if (schemeFiles.isEmpty()) {
      throw new SchemeImportException("There are no color schemes in the chosen file.");
    }
    return schemeFiles;
  }

  private boolean tryImportWithImportHandler(@NotNull String importerName) {
    for (ImportHandler importHandler : ImportHandler.EP_NAME.getExtensionList()) {
       if (importerName.equals(importHandler.getTitle())) {
         importHandler.performImport(getSchemesPanel().getToolbar(), scheme -> {
           if (scheme != null) getOptions().addImportedScheme(scheme);
         });
         return true;
       }
    }
    return false;
  }

  @Override
  protected void resetScheme(@NotNull EditorColorsScheme scheme) {
      if (Messages
            .showOkCancelDialog(ApplicationBundle.message("color.scheme.reset.message"),
                                ApplicationBundle.message("color.scheme.reset.title"), Messages.getQuestionIcon()) == Messages.OK) {
        getOptions().resetSchemeToOriginal(scheme.getName());
      }
  }

  @Override
  protected void duplicateScheme(@NotNull  EditorColorsScheme scheme, @NotNull String newName) {
      getOptions().saveSchemeAs(scheme, newName);
  }

  @Override
  protected void exportScheme(@Nullable Project project, @NotNull EditorColorsScheme scheme, @NotNull String exporterName) {
    EditorColorsScheme schemeToExport = scheme;
    if (scheme instanceof AbstractColorsScheme) {
      EditorColorsScheme parent = ((AbstractColorsScheme)scheme).getParentScheme();
      if (!(parent instanceof DefaultColorsScheme)) {
        schemeToExport = parent;
      }
    }
    schemeToExport = (EditorColorsScheme)schemeToExport.clone();
    schemeToExport.setName(SchemeManager.getDisplayName(schemeToExport));
    super.exportScheme(project, schemeToExport, exporterName);
  }

  @NotNull
  @Override
  protected Class<EditorColorsScheme> getSchemeType() {
    return EditorColorsScheme.class;
  }

  @NotNull
  protected abstract ColorAndFontOptions getOptions();

  private static class ImportSchemeChooserDialog extends DialogWrapper {

    private final Component myComponentAbove;
    private final List<ColorSchemeItem> mySchemeItems;
    private JBList<ColorSchemeItem> mySchemeList;

    protected ImportSchemeChooserDialog(@NotNull Component parent,
                                        @NotNull Component componentAbove,
                                        @NotNull List<ColorSchemeItem> schemeItems) {
      super(parent, false);
      setTitle(ApplicationBundle.message("settings.editor.scheme.import.chooser.title"));
      setOKButtonText(ApplicationBundle.message("settings.editor.scheme.import.chooser.button"));
      myComponentAbove = componentAbove;
      mySchemeItems = schemeItems;
      init();
    }

    @Nullable
    @Override
    public Point getInitialLocation() {
      Point location = myComponentAbove.getLocationOnScreen();
      location.translate(0, myComponentAbove.getHeight() + JBUIScale.scale(20));
      return location;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      JPanel schemesPanel = new JPanel(new BorderLayout());
      mySchemeList = new JBList<>(mySchemeItems);
      schemesPanel.add(mySchemeList, BorderLayout.CENTER);
      return schemesPanel;
    }

    public List<ColorSchemeItem> getSelectedItems() {
      int minIndex = mySchemeList.getMinSelectionIndex();
      int maxIndex = mySchemeList.getMaxSelectionIndex();
      if (minIndex >= 0 && maxIndex >= minIndex) {
        return mySchemeItems.subList(minIndex, maxIndex +1);
      }
      return Collections.emptyList();
    }
  }

  private static class ColorSchemeItem {
    private final String myName;
    private final VirtualFile myFile;

    ColorSchemeItem(String name, VirtualFile file) {
      myName = name;
      myFile = file;
    }

    public String getName() {
      return myName;
    }

    public VirtualFile getFile() {
      return myFile;
    }

    @Override
    public String toString() {
      return myName;
    }
  }
}
