/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.compiler.ant;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

/**
 * @author Eugene Zhuravlev
 */
public class ModuleSources extends CompositeGenerator{
  private final VirtualFile[] mySourceRoots = VirtualFile.EMPTY_ARRAY;
  private final VirtualFile[] myTestSourceRoots = VirtualFile.EMPTY_ARRAY;

  public ModuleSources(Module module, File baseDir, final GenerationOptions genOptions) {
    /*
    final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    final ModuleFileIndex moduleFileIndex = rootManager.getFileIndex();

    final List<VirtualFile> sourceRootFiles = new ArrayList<VirtualFile>();
    final List<VirtualFile> testSourceRootFiles = new ArrayList<VirtualFile>();

    final Path sourcepath = new Path(BuildProperties.getSourcepathProperty(module.getName()));
    final Path testSourcepath = new Path(BuildProperties.getTestSourcepathProperty(module.getName()));
    final PatternSet excludedFromModule = new PatternSet(BuildProperties.getExcludedFromModuleProperty(module.getName()));

    final ContentEntry[] contentEntries = rootManager.getContentEntries();
    for (int idx = 0; idx < contentEntries.length; idx++) {
      final ContentEntry contentEntry = contentEntries[idx];
      final VirtualFile file = contentEntry.getFile();
      if (file == null) {
        continue; // filter invalid entries
      }
      if (!(file.getFileSystem() instanceof LocalFileSystem)) {
        continue; // skip content roots inside jar and zip archives
      }
      final VirtualFile dirSetRoot = getDirSetRoot(contentEntry);

      final String dirSetRootRelativeToBasedir = GenerationUtils.toRelativePath(dirSetRoot, baseDir, BuildProperties.getModuleChunkBasedirProperty(module), genOptions, !module.isSavePathsRelative());
      final DirSet sourcesDirSet = new DirSet(dirSetRootRelativeToBasedir);
      final DirSet testSourcesDirSet = new DirSet(dirSetRootRelativeToBasedir);

      final VirtualFile[] sourceRoots = contentEntry.getSourceFolderFiles();
      for (int i = 0; i < sourceRoots.length; i++) {
        final VirtualFile root = sourceRoots[i];
        if (!moduleFileIndex.isInContent(root)) {
          continue; // skip library sources
        }

        addExcludePatterns(module, root, root, excludedFromModule, true);

        final Include include = new Include(VfsUtil.getRelativePath(root, dirSetRoot, '/'));
        if (moduleFileIndex.isInTestSourceContent(root)) {
          testSourcesDirSet.add(include);
          testSourceRootFiles.add(root);
        }
        else {
          sourcesDirSet.add(include);
          sourceRootFiles.add(root);
        }
      }
      if (sourcesDirSet.getGeneratorCount() > 0) {
        sourcepath.add(sourcesDirSet);
      }
      if (testSourcesDirSet.getGeneratorCount() > 0) {
        testSourcepath.add(testSourcesDirSet);
      }
    }

    mySourceRoots = sourceRootFiles.toArray(new VirtualFile[sourceRootFiles.size()]);
    myTestSourceRoots = testSourceRootFiles.toArray(new VirtualFile[testSourceRootFiles.size()]);

    final String moduleName = module.getName();

    add(excludedFromModule);

    final PatternSet excludedFromCompilation = new PatternSet(BuildProperties.getExcludedFromCompilationProperty(moduleName));
    excludedFromCompilation.add(new PatternSetRef(BuildProperties.getExcludedFromModuleProperty(moduleName)));
    excludedFromCompilation.add(new PatternSetRef(BuildProperties.PROPERTY_COMPILER_EXCLUDES));
    add(excludedFromCompilation, 1);

    if (sourcepath.getGeneratorCount() > 0) {
      add(sourcepath, 1);
    }
    if (testSourcepath.getGeneratorCount() != 0) {
      add(testSourcepath, 1);
    }
    */
  }

  private VirtualFile getDirSetRoot(final ContentEntry contentEntry) {
    final VirtualFile contentRoot = contentEntry.getFile();
    final VirtualFile[] sourceFolderFiles = contentEntry.getSourceFolderFiles();
    for (VirtualFile sourceFolderFile : sourceFolderFiles) {
      if (contentRoot.equals(sourceFolderFile)) {
        return contentRoot.getParent();
      }
    }
    return contentRoot;
  }

  public VirtualFile[] getSourceRoots() {
    return mySourceRoots;
  }

  public VirtualFile[] getTestSourceRoots() {
    return myTestSourceRoots;
  }

}
