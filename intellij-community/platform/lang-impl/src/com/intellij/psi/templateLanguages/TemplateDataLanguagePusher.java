// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.templateLanguages;

import com.intellij.FileIntPropertyPusher;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.openapi.vfs.newvfs.persistent.VfsDependentEnum;
import com.intellij.util.ObjectUtils;
import com.intellij.util.io.EnumeratorStringDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @author Konstantin.Ulitin
 */
public class TemplateDataLanguagePusher implements FileIntPropertyPusher<Language> {

  public static final Key<Language> KEY = Key.create("TEMPLATE_DATA_LANGUAGE");

  private static final VfsDependentEnum<String> ourLanguagesEnumerator = new VfsDependentEnum<>(
    "languages",
    EnumeratorStringDescriptor.INSTANCE,
    1
  );

  @NotNull
  @Override
  public Key<Language> getFileDataKey() {
    return KEY;
  }

  @Override
  public boolean pushDirectoriesOnly() {
    return false;
  }

  @NotNull
  @Override
  public Language getDefaultValue() {
    return Language.ANY;
  }

  @Nullable
  @Override
  public Language getImmediateValue(@NotNull Project project, @Nullable VirtualFile file) {
    return TemplateDataLanguageMappings.getInstance(project).getImmediateMapping(file);
  }

  @Nullable
  @Override
  public Language getImmediateValue(@NotNull Module module) {
    return null;
  }

  @Override
  public boolean acceptsFile(@NotNull VirtualFile file, @NotNull Project project) {
    FileType type = file.getFileType();
    return type instanceof LanguageFileType && ((LanguageFileType)type).getLanguage() instanceof TemplateLanguage;
  }

  @Override
  public boolean acceptsDirectory(@NotNull VirtualFile file, @NotNull Project project) {
    return true;
  }

  private static final FileAttribute PERSISTENCE = new FileAttribute("template_language", 2, true);

  @Override
  public @NotNull FileAttribute getAttribute() {
    return PERSISTENCE;
  }

  @Override
  public int toInt(@NotNull Language property) throws IOException {
    return ourLanguagesEnumerator.getId(property.getID());
  }

  @NotNull
  @Override
  public Language fromInt(int val) throws IOException {
    String id = ourLanguagesEnumerator.getById(val);
    Language lang = Language.findLanguageByID(id);
    return ObjectUtils.notNull(lang, Language.ANY);
  }

  @Override
  public void propertyChanged(@NotNull Project project, @NotNull VirtualFile fileOrDir, @NotNull Language actualProperty) {
    PushedFilePropertiesUpdater.getInstance(project).filePropertiesChanged(fileOrDir, file -> acceptsFile(file, project));
  }
}
