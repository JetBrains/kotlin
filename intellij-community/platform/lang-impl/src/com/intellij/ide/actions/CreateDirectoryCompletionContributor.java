// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.Collection;

/**
 * Implement this contributor to add useful and often used directories in the 'Create Directory' popup.<br>
 * E.g. Gradle or Maven could add conventional source directories ('src/main/java', 'src/main/test')
 */
public interface CreateDirectoryCompletionContributor {
  /**
   * @return A short description for the suggested variants, to be shown as a group's title in the completion list.<br>
   * E.g 'Gradle Source Sets', 'Maven Source Directories'
   */
  @NotNull
  String getDescription();

  /**
   * @return completion subdirectory variant for the selected directory
   */
  @NotNull
  Collection<Variant> getVariants(@NotNull PsiDirectory directory);

  final class Variant {
    @NotNull final String path;
    @Nullable final JpsModuleSourceRootType<?> rootType;

    /**
     * @param path absolute or relative path to a directory
     * @param rootType root type with which the created directory will be marked automatically marked
     */
    public Variant(@NotNull String path, @Nullable JpsModuleSourceRootType<?> rootType) {
      this.path = path;
      this.rootType = rootType;
    }

    @NotNull
    public String getPath() {
      return path;
    }

    @Nullable
    public JpsModuleSourceRootType<?> getRootType() {
      return rootType;
    }
  }
}
