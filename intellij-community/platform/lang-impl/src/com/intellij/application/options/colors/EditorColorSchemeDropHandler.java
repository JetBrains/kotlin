// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors;

import com.intellij.application.options.schemes.SchemeNameGenerator;
import com.intellij.ide.actions.QuickChangeColorSchemeAction;
import com.intellij.ide.dnd.FileCopyPasteUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CustomFileDropHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme;
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl;
import com.intellij.openapi.editor.colors.impl.EmptyColorScheme;
import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class EditorColorSchemeDropHandler extends CustomFileDropHandler {

  public static final String ADDED = "Color scheme added";

  @Override
  public boolean canHandle(@NotNull Transferable t, @Nullable Editor editor) {
    return getColorSchemeFile(t) != null;
  }

  private static VirtualFile getColorSchemeFile(@NotNull Transferable t) {
    List<File> list = FileCopyPasteUtil.getFileList(t);
    final File io = list != null && list.size() == 1 ? ContainerUtil.getFirstItem(list) : null;
    if (io == null || !StringUtil.endsWithIgnoreCase(io.getName(), ".icls")) return null;

    return VfsUtil.findFileByIoFile(io, true);
  }

  @Override
  public boolean handleDrop(@NotNull Transferable t, @Nullable Editor editor, Project project) {
    VirtualFile file = getColorSchemeFile(t);
    assert file != null;

    if (Messages.YES == Messages.showYesNoDialog("Would you like to install and apply '" + file.getName() + "' editor color scheme?",
                                                 "Install Color Scheme?",
                                                 "Install",
                                                 "Open in Editor",
                                                 null)) {
      try {
        ColorSchemeImporter importer = new ColorSchemeImporter();
        EditorColorsManager colorsManager = EditorColorsManager.getInstance();
        List<String> names = ContainerUtil.map(colorsManager.getAllSchemes(), EditorColorsScheme::getName);
        EditorColorsScheme imported = importer
          .importScheme(DefaultProjectFactory.getInstance().getDefaultProject(), file, colorsManager.getGlobalScheme(),
                        name -> {
                          String preferredName = name != null ? name : "Unnamed";
                          String newName = SchemeNameGenerator.getUniqueName(preferredName, candidate -> names.contains(candidate));
                          AbstractColorsScheme newScheme = new EditorColorsSchemeImpl(EmptyColorScheme.INSTANCE);
                          newScheme.setName(newName);
                          newScheme.setDefaultMetaInfo(EmptyColorScheme.INSTANCE);
                          return newScheme;
                        });
        if (imported != null) {
          colorsManager.addColorsScheme(imported);
          String message = importer.getAdditionalImportInfo(imported);
          if (message == null) {
            message = ApplicationBundle.message("settings.editor.scheme.import.success", file.getPresentableUrl(), imported.getName());
          }

          colorsManager.setGlobalScheme(imported);
          Notification notification = new Notification("", ADDED, message, NotificationType.INFORMATION);
          QuickChangeColorSchemeAction.changeLafIfNecessary(imported,
                                                            () -> new Alarm().addRequest(
                                                              () -> Notifications.Bus.notify(notification, project), 300));
        }
      }
      catch (SchemeImportException e) {
        String title = e.isWarning() ? ADDED : "Color scheme import failed";
        NotificationType type = e.isWarning() ? NotificationType.WARNING : NotificationType.ERROR;
        Notification notification = new Notification("", title, e.getMessage(), type);
        notification.notify(project);
      }
      return true;
    }

    return false;
  }
}
