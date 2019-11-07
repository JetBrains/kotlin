/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.packaging.impl.artifacts;

import com.intellij.CommonBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ArtifactTemplate;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.impl.elements.LibraryPackagingElement;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.packaging.impl.elements.ProductionModuleOutputElementType;
import com.intellij.packaging.impl.elements.TestModuleOutputElementType;
import com.intellij.util.CommonProcessors;
import gnu.trove.THashSet;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * @author nik
 */
public class JarFromModulesTemplate extends ArtifactTemplate {
  private static final Logger LOG = Logger.getInstance(JarFromModulesTemplate.class);

  private final PackagingElementResolvingContext myContext;

  public JarFromModulesTemplate(PackagingElementResolvingContext context) {
    myContext = context;
  }

  @Override
  public NewArtifactConfiguration createArtifact() {
    JarArtifactFromModulesDialog dialog = new JarArtifactFromModulesDialog(myContext);
    if (!dialog.showAndGet()) {
      return null;
    }

    return doCreateArtifact(dialog.getSelectedModules(), dialog.getMainClassName(), dialog.getDirectoryForManifest(),
                            dialog.isExtractLibrariesToJar(), dialog.isIncludeTests());
  }

  @Nullable
  public NewArtifactConfiguration doCreateArtifact(final Module[] modules, final String mainClassName,
                                                   final String directoryForManifest,
                                                   final boolean extractLibrariesToJar,
                                                   final boolean includeTests) {
    VirtualFile manifestFile = null;
    final Project project = myContext.getProject();
    if (mainClassName != null && !mainClassName.isEmpty() || !extractLibrariesToJar) {
      final VirtualFile directory;
      try {
        directory = ApplicationManager.getApplication().runWriteAction(
          (ThrowableComputable<VirtualFile, IOException>)() -> VfsUtil.createDirectoryIfMissing(directoryForManifest));
      }
      catch (IOException e) {
        LOG.info(e);
        Messages.showErrorDialog(project, "Cannot create directory '" + directoryForManifest + "': " + e.getMessage(),
                                 CommonBundle.getErrorTitle());
        return null;
      }
      if (directory == null) return null;

      manifestFile = ManifestFileUtil.createManifestFile(directory, project);
      if (manifestFile == null) {
        return null;
      }
      ManifestFileUtil.updateManifest(manifestFile, mainClassName, null, true);
    }

    String name = modules.length == 1 ? modules[0].getName() : project.getName();

    final PackagingElementFactory factory = PackagingElementFactory.getInstance();
    final CompositePackagingElement<?> archive = factory.createArchive(ArtifactUtil.suggestArtifactFileName(name) + ".jar");

    OrderEnumerator orderEnumerator = ProjectRootManager.getInstance(project).orderEntries(Arrays.asList(modules));

    final Set<Library> libraries = new THashSet<>();
    if (!includeTests) {
      orderEnumerator = orderEnumerator.productionOnly();
    }
    final ModulesProvider modulesProvider = myContext.getModulesProvider();
    final OrderEnumerator enumerator = orderEnumerator.using(modulesProvider).withoutSdk().runtimeOnly().recursively();
    enumerator.forEachLibrary(new CommonProcessors.CollectProcessor<>(libraries));
    enumerator.forEachModule(module -> {
      if (ProductionModuleOutputElementType.ELEMENT_TYPE.isSuitableModule(modulesProvider, module)) {
        archive.addOrFindChild(factory.createModuleOutput(module));
      }
      if (includeTests && TestModuleOutputElementType.ELEMENT_TYPE.isSuitableModule(modulesProvider, module)) {
        archive.addOrFindChild(factory.createTestModuleOutput(module));
      }
      return true;
    });

    final JarArtifactType jarArtifactType = JarArtifactType.getInstance();
    if (manifestFile != null && !manifestFile.equals(ManifestFileUtil.findManifestFile(archive, myContext, jarArtifactType))) {
      archive.addFirstChild(factory.createFileCopyWithParentDirectories(manifestFile.getPath(), ManifestFileUtil.MANIFEST_DIR_NAME));
    }

    final String artifactName = name + ":jar";
    if (extractLibrariesToJar) {
      addExtractedLibrariesToJar(archive, factory, libraries);
      return new NewArtifactConfiguration(archive, artifactName, jarArtifactType);
    }
    else {
      final ArtifactRootElement<?> root = factory.createArtifactRootElement();
      List<String> classpath = new ArrayList<>();
      root.addOrFindChild(archive);
      addLibraries(libraries, root, archive, classpath);
      ManifestFileUtil.updateManifest(manifestFile, mainClassName, classpath, true);
      return new NewArtifactConfiguration(root, artifactName, PlainArtifactType.getInstance());
    }
  }

  private void addLibraries(Set<? extends Library> libraries, ArtifactRootElement<?> root, CompositePackagingElement<?> archive,
                            List<? super String> classpath) {
    PackagingElementFactory factory = PackagingElementFactory.getInstance();
    for (Library library : libraries) {
      if (LibraryPackagingElement.getKindForLibrary(library).containsDirectoriesWithClasses()) {
        for (VirtualFile classesRoot : library.getFiles(OrderRootType.CLASSES)) {
          if (classesRoot.isInLocalFileSystem()) {
            archive.addOrFindChild(factory.createDirectoryCopyWithParentDirectories(classesRoot.getPath(), "/"));
          }
          else {
            final PackagingElement<?> child = factory.createFileCopyWithParentDirectories(VfsUtil.getLocalFile(classesRoot).getPath(), "/");
            root.addOrFindChild(child);
            classpath.addAll(ManifestFileUtil.getClasspathForElements(Collections.singletonList(child), myContext, PlainArtifactType.getInstance()));
          }
        }

      }
      else {
        final List<? extends PackagingElement<?>> children = factory.createLibraryElements(library);
        classpath.addAll(ManifestFileUtil.getClasspathForElements(children, myContext, PlainArtifactType.getInstance()));
        root.addOrFindChildren(children);
      }
    }
  }

  private static void addExtractedLibrariesToJar(CompositePackagingElement<?> archive, PackagingElementFactory factory, Set<? extends Library> libraries) {
    for (Library library : libraries) {
      if (LibraryPackagingElement.getKindForLibrary(library).containsJarFiles()) {
        for (VirtualFile classesRoot : library.getFiles(OrderRootType.CLASSES)) {
          if (classesRoot.isInLocalFileSystem()) {
            archive.addOrFindChild(factory.createDirectoryCopyWithParentDirectories(classesRoot.getPath(), "/"));
          }
          else {
            archive.addOrFindChild(factory.createExtractedDirectory(classesRoot));
          }
        }

      }
      else {
        archive.addOrFindChildren(factory.createLibraryElements(library));
      }
    }
  }

  @Override
  public String getPresentableName() {
    return "From modules with dependencies...";
  }
}
