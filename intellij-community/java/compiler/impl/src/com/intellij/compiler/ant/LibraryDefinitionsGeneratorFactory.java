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

import com.intellij.compiler.ant.taskdefs.FileSet;
import com.intellij.compiler.ant.taskdefs.Path;
import com.intellij.compiler.ant.taskdefs.PathElement;
import com.intellij.compiler.ant.taskdefs.PatternSetRef;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 */
public class LibraryDefinitionsGeneratorFactory {
  private final ProjectEx myProject;
  private final GenerationOptions myGenOptions;
  private final Set<String> myUsedLibraries = new HashSet<>();

  public LibraryDefinitionsGeneratorFactory(ProjectEx project, GenerationOptions genOptions) {
    myProject = project;
    myGenOptions = genOptions;
    final ModuleManager moduleManager = ModuleManager.getInstance(project);
    final Module[] modules = moduleManager.getModules();
    for (Module module : modules) {
      ModuleRootManager.getInstance(module).orderEntries().forEachLibrary(library -> {
        final String name = library.getName();
        if (name != null) {
          myUsedLibraries.add(name);
        }
        return true;
      });
    }
  }

  /**
   * Create a generator for the specified libary type. It generates a list of library definitions.
   *
   * @param libraryTable a library table to examine
   * @param baseDir      base directory for ant build script
   * @param comment      a comment to use for the library
   * @return the created generator or null if there is a nothing to generate
   */
  @Nullable
  public Generator create(LibraryTable libraryTable, File baseDir, final String comment) {
    final Library[] libraries = libraryTable.getLibraries();
    if (libraries.length == 0) {
      return null;
    }

    final CompositeGenerator gen = new CompositeGenerator();

    gen.add(new Comment(comment), 1);
    // sort libraries to ensure stable order of them.
    TreeMap<String, Library> sortedLibs = new TreeMap<>();
    for (final Library library : libraries) {
      final String libraryName = library.getName();
      if (!myUsedLibraries.contains(libraryName)) {
        continue;
      }
      sortedLibs.put(BuildProperties.getLibraryPathId(libraryName), library);
    }
    for (final Library library : sortedLibs.values()) {
      final String libraryName = library.getName();
      final Path libraryPath = new Path(BuildProperties.getLibraryPathId(libraryName));
      genLibraryContent(myProject, myGenOptions, library, baseDir, libraryPath);
      gen.add(libraryPath, 1);
    }
    return gen.getGeneratorCount() > 0 ? gen : null;
  }

  /**
   * Generate library content
   *
   * @param project     the context project
   * @param genOptions  the generation options
   * @param library     the library which content is generated
   * @param baseDir     the base directory
   * @param libraryPath the composite generator to update
   */
  public static void genLibraryContent(final ProjectEx project,
                                       final GenerationOptions genOptions,
                                       final Library library,
                                       final File baseDir,
                                       final CompositeGenerator libraryPath) {
    genLibraryContent(genOptions, library, OrderRootType.CLASSES, baseDir, libraryPath);
  }

  public static void genLibraryContent(final GenerationOptions genOptions,
                                       final Library library,
                                       final OrderRootType rootType, final File baseDir,
                                       final CompositeGenerator libraryPath) {
    if (genOptions.expandJarDirectories) {
      final VirtualFile[] files = library.getFiles(rootType);
      // note that it is assumed that directory entries inside library path are unordered
      TreeSet<String> visitedPaths = new TreeSet<>();
      for (final VirtualFile file : files) {
        final String path = GenerationUtils
          .toRelativePath(file, baseDir, BuildProperties.getProjectBaseDirProperty(), genOptions);
        visitedPaths.add(path);
      }
      for (final String path : visitedPaths) {
        libraryPath.add(new PathElement(path));
      }
    }
    else {
      TreeSet<String> urls = new TreeSet<>(Arrays.asList(library.getUrls(rootType)));
      for (String url : urls) {
        File file = fileFromUrl(url);
        final String path = GenerationUtils
          .toRelativePath(file.getPath(), baseDir, BuildProperties.getProjectBaseDirProperty(), genOptions);
        if (url.startsWith(JarFileSystem.PROTOCOL_PREFIX)) {
          libraryPath.add(new PathElement(path));
        }
        else if (url.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
          if (library.isJarDirectory(url, rootType)) {
            final FileSet fileSet = new FileSet(path);
            fileSet.add(new PatternSetRef(BuildProperties.PROPERTY_LIBRARIES_PATTERNS));
            libraryPath.add(fileSet);
          }
          else {
            libraryPath.add(new PathElement(path));
          }
        }
        else {
          throw new IllegalStateException("Unknown url type: " + url);
        }
      }
    }
  }

  /**
   * Gets file from jar of file URL
   *
   * @param url an url to parse
   * @return
   */
  private static File fileFromUrl(String url) {
    final String filePart;
    if (url.startsWith(JarFileSystem.PROTOCOL_PREFIX) && url.endsWith(JarFileSystem.JAR_SEPARATOR)) {
      filePart = url.substring(JarFileSystem.PROTOCOL_PREFIX.length(), url.length() - JarFileSystem.JAR_SEPARATOR.length());
    }
    else if (url.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
      filePart = url.substring(JarFileSystem.PROTOCOL_PREFIX.length());
    }
    else {
      throw new IllegalArgumentException("Unknown url type: " + url);
    }
    return new File(filePart.replace('/', File.separatorChar));
  }
}
