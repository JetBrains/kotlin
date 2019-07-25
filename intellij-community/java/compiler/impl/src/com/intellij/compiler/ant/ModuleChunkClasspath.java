// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Path;
import com.intellij.compiler.ant.taskdefs.PathElement;
import com.intellij.compiler.ant.taskdefs.PathRef;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.OrderedSet;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Generator of module chunk classspath. The class used to generate both runtime and compile time classpaths.
 *
 * @author Eugene Zhuravlev
 */
public class ModuleChunkClasspath extends Path {

  /**
   * A constructor
   *
   * @param chunk                    a chunk to process
   * @param genOptions               a generation options
   * @param generateRuntimeClasspath if true, runtime classpath is being generated. Otherwise a compile time classpath is constructed
   * @param generateTestClasspath    if true, a test classpath is generated.
   */
  public ModuleChunkClasspath(final ModuleChunk chunk,
                              final GenerationOptions genOptions,
                              final boolean generateRuntimeClasspath,
                              final boolean generateTestClasspath) {
    super(generateClasspathName(chunk, generateRuntimeClasspath, generateTestClasspath));

    final OrderedSet<ClasspathItem> pathItems =
      new OrderedSet<>();
    final String moduleChunkBasedirProperty = BuildProperties.getModuleChunkBasedirProperty(chunk);
    final Module[] modules = chunk.getModules();
    // processed chunks (used only in runtime classpath), every chunk is referenced exactly once
    final Set<ModuleChunk> processedChunks = new HashSet<>();
    // pocessed modules
    final Set<Module> processedModules = new HashSet<>();
    for (final Module module : modules) {
      new Object() {
        /**
         * Process the module. The logic is different for compile-time case and runtime case.
         * In the case of runtime, only directly referenced objects are included in classpath.
         * Indirectly referenced are
         *
         * @param module a module to process.
         * @param dependencyLevel is increased with every of recursion.
         * @param isModuleExported if true the module is exported from the previous level
         */
        public void processModule(final Module module, final int dependencyLevel, final boolean isModuleExported) {
          if (processedModules.contains(module)) {
            // the module is already processed, nothing should be done
            return;
          }
          if (dependencyLevel > 1 && !isModuleExported && !(genOptions.inlineRuntimeClasspath && generateRuntimeClasspath)) {
            // the module is not in exports and it is not directly included skip it in the case of library pathgeneration
            return;
          }
          processedModules.add(module);
          final ProjectEx project = (ProjectEx)chunk.getProject();
          final File baseDir = BuildProperties.getProjectBaseDir(project);
          OrderEnumerator enumerator = ModuleRootManager.getInstance(module).orderEntries();
          if (generateRuntimeClasspath) {
            enumerator = enumerator.runtimeOnly();
          }
          else {
            enumerator = enumerator.compileOnly();
            if (!generateTestClasspath && (dependencyLevel == 0 || chunk.contains(module))) {
              // this is the entry for outpath of the currently processed module
              // the root module is never included
              enumerator = enumerator.withoutModuleSourceEntries();
            }
          }
          if (!generateTestClasspath) {
            enumerator = enumerator.productionOnly();
          }
          enumerator.forEach(orderEntry -> {
            if (!orderEntry.isValid()) {
              return true;
            }

            if (!generateRuntimeClasspath &&
                !(orderEntry instanceof ModuleOrderEntry) &&
                !(orderEntry instanceof ModuleSourceOrderEntry)) {
              // needed for compilation classpath only
              final boolean isExported = (orderEntry instanceof ExportableOrderEntry) && ((ExportableOrderEntry)orderEntry).isExported();
              if (dependencyLevel > 0 && !isExported) {
                // non-exported dependencies are excluded and not processed
                return true;
              }
            }

            if (orderEntry instanceof JdkOrderEntry) {
              if (genOptions.forceTargetJdk && !generateRuntimeClasspath) {
                pathItems
                  .add(new PathRefItem(BuildProperties.propertyRef(BuildProperties.getModuleChunkJdkClasspathProperty(chunk.getName()))));
              }
            }
            else if (orderEntry instanceof ModuleOrderEntry) {
              final ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)orderEntry;
              final Module dependentModule = moduleOrderEntry.getModule();
              if (!chunk.contains(dependentModule)) {
                if (generateRuntimeClasspath && !genOptions.inlineRuntimeClasspath) {
                  // in case of runtime classpath, just an referenced to corresponding classpath is created
                  final ModuleChunk depChunk = genOptions.getChunkByModule(dependentModule);
                  if (!processedChunks.contains(depChunk)) {
                    // chunk references are included in the runtime classpath only once
                    processedChunks.add(depChunk);
                    String property = generateTestClasspath ? BuildProperties.getTestRuntimeClasspathProperty(depChunk.getName())
                                                            : BuildProperties.getRuntimeClasspathProperty(depChunk.getName());
                    pathItems.add(new PathRefItem(property));
                  }
                }
                else {
                  // in case of compile classpath or inlined runtime classpath,
                  // the referenced module is processed recursively
                  processModule(dependentModule, dependencyLevel + 1, moduleOrderEntry.isExported());
                }
              }
            }
            else if (orderEntry instanceof LibraryOrderEntry) {
              final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
              final String libraryName = libraryOrderEntry.getLibraryName();
              if (((LibraryOrderEntry)orderEntry).isModuleLevel()) {
                CompositeGenerator gen = new CompositeGenerator();
                gen.setHasLeadingNewline(false);
                LibraryDefinitionsGeneratorFactory.genLibraryContent(project, genOptions, libraryOrderEntry.getLibrary(), baseDir, gen);
                pathItems.add(new GeneratorItem(libraryName, gen));
              }
              else {
                pathItems.add(new PathRefItem(BuildProperties.getLibraryPathId(libraryName)));
              }
            }
            else if (orderEntry instanceof ModuleSourceOrderEntry) {
              // Module source entry?
              for (String url : getCompilationClasses(module, ((GenerationOptionsImpl)genOptions), generateRuntimeClasspath,
                                                      generateTestClasspath, dependencyLevel == 0)) {
                url = StringUtil.trimEnd(url, JarFileSystem.JAR_SEPARATOR);
                final String propertyRef = genOptions.getPropertyRefForUrl(url);
                if (propertyRef != null) {
                  pathItems.add(new PathElementItem(propertyRef));
                }
                else {
                  final String path = VirtualFileManager.extractPath(url);
                  pathItems.add(new PathElementItem(
                    GenerationUtils.toRelativePath(path, chunk.getBaseDir(), moduleChunkBasedirProperty, genOptions)));
                }
              }
            }
            else {
              // Unknown order entry type. If it is actually encountered, extension point should be implemented
              pathItems.add(new GeneratorItem(orderEntry.getClass().getName(),
                                              new Comment("Unknown OrderEntryType: " + orderEntry.getClass().getName())));
            }
            return true;
          });
        }
      }.processModule(module, 0, false);
    }
    // convert path items to generators
    for (final ClasspathItem pathItem : pathItems) {
      add(pathItem.toGenerator());
    }
  }

  /**
   * Generate classpath name
   *
   * @param chunk                    a chunk
   * @param generateRuntimeClasspath
   * @param generateTestClasspath
   * @return a name for the classpath
   */
  private static String generateClasspathName(ModuleChunk chunk, boolean generateRuntimeClasspath, boolean generateTestClasspath) {
    if (generateTestClasspath) {
      return generateRuntimeClasspath
             ? BuildProperties.getTestRuntimeClasspathProperty(chunk.getName())
             : BuildProperties.getTestClasspathProperty(chunk.getName());
    }
    else {
      return generateRuntimeClasspath
             ? BuildProperties.getRuntimeClasspathProperty(chunk.getName())
             : BuildProperties.getClasspathProperty(chunk.getName());
    }
  }

  private static String[] getCompilationClasses(final Module module,
                                                final GenerationOptionsImpl options,
                                                final boolean forRuntime,
                                                final boolean forTest,
                                                final boolean firstLevel) {
    final CompilerModuleExtension extension = CompilerModuleExtension.getInstance(module);
    if (extension == null) return ArrayUtilRt.EMPTY_STRING_ARRAY;

    if (!forRuntime) {
      if (forTest) {
        return extension.getOutputRootUrls(!firstLevel);
      }
      else {
        return firstLevel ? ArrayUtilRt.EMPTY_STRING_ARRAY : extension.getOutputRootUrls(false);
      }
    }
    final Set<String> jdkUrls = options.getAllJdkUrls();

    final OrderedSet<String> urls = new OrderedSet<>();
    ContainerUtil.addAll(urls, extension.getOutputRootUrls(forTest));
    urls.removeAll(jdkUrls);
    return ArrayUtilRt.toStringArray(urls);
  }

  /**
   * The base class for an item in the class path. Instances of the subclasses are used instead
   * of generators when building the class path content. The subclasses implement {@link Object#equals(Object)}
   * and {@link Object#hashCode()} methods in order to eliminate duplicates when building classpath.
   */
  private abstract static class ClasspathItem {
    /**
     * primary reference or path of the element
     */
    protected final String myValue;

    /**
     * A constructor
     *
     * @param value primary value of the element
     */
    ClasspathItem(String value) {
      myValue = value;
    }

    /**
     * @return a generator for path elements.
     */
    public abstract Generator toGenerator();
  }

  /**
   * Class path item that directly embeds generator
   */
  private static class GeneratorItem extends ClasspathItem {
    /**
     * An embedded generator
     */
    final Generator myGenerator;

    /**
     * A constructor
     *
     * @param value     primary value of the element
     * @param generator a generator to use
     */
    GeneratorItem(String value, final Generator generator) {
      super(value);
      myGenerator = generator;
    }

    @Override
    public Generator toGenerator() {
      return myGenerator;
    }
  }

  /**
   * This path element directly references some location.
   */
  private static class PathElementItem extends ClasspathItem {
    /**
     * A constructor
     *
     * @param value a referenced location
     */
    PathElementItem(String value) {
      super(value);
    }

    @Override
    public Generator toGenerator() {
      return new PathElement(myValue);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PathElementItem)) return false;

      final PathElementItem pathElementItem = (PathElementItem)o;

      if (!myValue.equals(pathElementItem.myValue)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myValue.hashCode();
    }
  }

  /**
   * This path element references a path
   */
  private static class PathRefItem extends ClasspathItem {
    /**
     * A constructor
     *
     * @param value an indentifier of referenced classpath
     */
    PathRefItem(String value) {
      super(value);
    }

    @Override
    public Generator toGenerator() {
      return new PathRef(myValue);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PathRefItem)) return false;

      final PathRefItem pathRefItem = (PathRefItem)o;

      if (!myValue.equals(pathRefItem.myValue)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myValue.hashCode();
    }
  }
}
