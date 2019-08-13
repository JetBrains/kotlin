/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.psi.templateLanguages;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.InjectableLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.LanguagePerFileMappings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.FilePropertyPusher;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author peter
 */
@State(name = "TemplateDataLanguageMappings", storages = @Storage("templateLanguages.xml"))
public class TemplateDataLanguageMappings extends LanguagePerFileMappings<Language> {

  public static TemplateDataLanguageMappings getInstance(final Project project) {
    return ServiceManager.getService(project, TemplateDataLanguageMappings.class);
  }

  public TemplateDataLanguageMappings(final Project project) {
    super(project);
  }

  @Override
  protected String serialize(final Language language) {
    return language.getID();
  }

  @Override
  public List<Language> getAvailableValues() {
    return getTemplateableLanguages();
  }

  @Nullable
  @Override
  public Language getMapping(@Nullable VirtualFile file) {
    Language t = getConfiguredMapping(file);
    return t == null || t == Language.ANY ? getDefaultMapping(file) : t;
  }

  @Override
  public Language getDefaultMapping(@Nullable VirtualFile file) {
    return getDefaultMappingForFile(file);
  }

  @Nullable
  public static Language getDefaultMappingForFile(@Nullable VirtualFile file) {
    return file == null? null : TemplateDataLanguagePatterns.getInstance().getTemplateDataLanguageByFileName(file);
  }

  public static List<Language> getTemplateableLanguages() {
    return ContainerUtil.findAll(Language.getRegisteredLanguages(), new Condition<Language>() {
      @Override
      public boolean value(final Language language) {
        if (language == Language.ANY) return false;
        if (language instanceof TemplateLanguage || language instanceof DependentLanguage || language instanceof InjectableLanguage) return false;
        if (language.getBaseLanguage() != null) return value(language.getBaseLanguage());
        return true;
      }
    });
  }

  private final FilePropertyPusher<Language> myPropertyPusher = new TemplateDataLanguagePusher();

  @NotNull
  @Override
  protected FilePropertyPusher<Language> getFilePropertyPusher() {
    return myPropertyPusher;
  }
}
