// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.artifacts.ui;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.artifacts.ArtifactsTestUtil;
import com.intellij.compiler.artifacts.PackagingElementsTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactTemplate;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.intellij.packaging.impl.artifacts.JarFromModulesTemplate;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class JarFromModulesTemplateTest extends PackagingElementsTestCase {
  private Artifact myArtifact;

  @Override
  protected void tearDown() throws Exception {
    myArtifact = null;
    super.tearDown();
  }

  @Override
  protected void setUpProject() throws Exception {
    super.setUpProject();
    CompilerConfiguration.getInstance(getProject()).addResourceFilePattern("?*.MF");
  }

  public void testSimpleModule() {
    final Module a = addModuleWithSourceRoot("a");
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a");
  }

  private Module addModuleWithSourceRoot(final String name) {
    return addModule(name, createDir("src-" + name));
  }

  public void testSimpleModuleWithMainClass() {
    final VirtualFile file = createFile("src/A.java");
    final Module a = addModule("a", file.getParent());
    createFromTemplate(a, "A", file.getParent().getPath(), true);

    assertLayout("a.jar\n" +
                 " module:a");
    assertManifest("A", null);
  }

  public void testSimpleModuleWithExternalManifest() {
    final VirtualFile file = createFile("src/A.java");
    final VirtualFile baseDir = file.getParent().getParent();
    final Module a = addModule("a", file.getParent());
    createFromTemplate(a, "A", baseDir.getPath(), true);

    assertLayout("a.jar\n" +
                 " META-INF/\n" +
                 "  file:" + baseDir.getPath() + "/META-INF/MANIFEST.MF\n" +
                 " module:a");
    assertManifest("A", null);

  }

  public void testModuleWithLibraryJar() {
    final Module module = addModuleWithSourceRoot("a");
    addProjectLibrary(module, "jdom", getJDomJar());
    createFromTemplate(module, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a\n" +
                 " extracted:" + getLocalJarPath(getJDomJar()) + "!/");
  }

  public void testModuleWithLibraryJarWithManifest() {
    final VirtualFile file = createFile("src/A.java");
    final Module module = addModule("a", file.getParent());
    VirtualFile jDomJar = getJDomJar();
    addProjectLibrary(module, "jdom", jDomJar);
    createFromTemplate(module, null, file.getParent().getPath(), false);
    assertLayout("<root>\n" +
                 " a.jar\n" +
                 "  module:a\n" +
                 " lib:jdom(project)");
    assertManifest(null, jDomJar.getName());
  }

  public void testSkipTestLibrary() {
    final Module a = addModuleWithSourceRoot("a");
    addProjectLibrary(a, "jdom", DependencyScope.TEST, getJDomJar());
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a");
  }

  public void testSkipProvidedLibrary() {
    final Module a = addModuleWithSourceRoot("a");
    addProjectLibrary(a, "jdom", DependencyScope.PROVIDED, getJDomJar());
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a");
  }

  public void testIncludeTests() {
    final Module a = addModuleWithSourceRoot("a");
    PsiTestUtil.addSourceRoot(a, createDir("testSrc-a"), true);
    addProjectLibrary(a, "jdom", DependencyScope.TEST, getJDomJar());
    createFromTemplate(a, null, null, true, true);
    assertLayout("a.jar\n" +
                 " module:a\n" +
                 " module-tests:a\n" +
                 " extracted:" + getLocalJarPath(getJDomJar()) + "!/");
  }

  public void testDoNotIncludeTestsForModuleWithoutTestSources() {
    final Module a = addModuleWithSourceRoot("a");
    addProjectLibrary(a, "jdom", DependencyScope.TEST, getJDomJar());
    createFromTemplate(a, null, null, true, true);
    assertLayout("a.jar\n" +
                 " module:a\n" +
                 " extracted:" + getLocalJarPath(getJDomJar()) + "!/");
  }

  public void testTwoIndependentModules() {
    final Module a = addModuleWithSourceRoot("a");
    addModuleWithSourceRoot("b");
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a");
  }

  public void testJarForProject() {
    addModuleWithSourceRoot("a");
    addModuleWithSourceRoot("b");
    createFromTemplate(null, null, null, true);
    assertLayout(getProject().getName() + ".jar\n" +
                 " module:a\n" +
                 " module:b");
  }

  public void testDependentModule() {
    final Module a = addModuleWithSourceRoot("a");
    final Module b = addModuleWithSourceRoot("b");
    addModuleDependency(a, b);
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a\n" +
                 " module:b");
  }

  public void testDependentModuleWithLibrary() {
    final Module a = addModuleWithSourceRoot("a");
    final Module b = addModuleWithSourceRoot("b");
    addModuleDependency(a, b);
    addProjectLibrary(b, "jdom", getJDomJar());
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a\n" +
                 " module:b\n" +
                 " extracted:" + getLocalJarPath(getJDomJar()) + "!/");
  }

  public void testExtractedLibraryWithDirectories() {
    final VirtualFile dir = createDir("lib");
    final Module a = addModuleWithSourceRoot("a");
    addProjectLibrary(a, "dir", dir);
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a\n" +
                 " lib:dir(project)");
  }

  public void testCopiedLibraryWithDirectories() {
    final VirtualFile dir = createDir("lib");
    final Module a = addModuleWithSourceRoot("a");
    addProjectLibrary(a, "dir", dir);
    final String basePath = myProject.getBasePath();
    createFromTemplate(a, null, basePath, false);
    assertLayout("<root>\n" +
                 " a.jar\n" +
                 "  META-INF/\n" +
                 "   file:" + basePath + "/META-INF/MANIFEST.MF\n" +
                 "  module:a\n" +
                 "  dir:" + dir.getPath());
  }

  public void testExtractedLibraryWithJarsAndDirs() {
    final VirtualFile dir = createDir("lib");
    final Module a = addModuleWithSourceRoot("a");
    addProjectLibrary(a, "dir", dir, getJDomJar());
    createFromTemplate(a, null, null, true);
    assertLayout("a.jar\n" +
                 " module:a\n" +
                 " dir:" + dir.getPath() + "\n" +
                 " extracted:" + getLocalJarPath(getJDomJar()) + "!/");
  }

  public void testCopiedLibraryWithJarsAndDirs() {
    final VirtualFile dir = createDir("lib");
    final Module a = addModuleWithSourceRoot("a");
    VirtualFile jDomJar = getJDomJar();
    addProjectLibrary(a, "dir", dir, jDomJar);
    final String basePath = myProject.getBasePath();
    createFromTemplate(a, null, basePath, false);
    assertLayout("<root>\n" +
                 " a.jar\n" +
                 "  META-INF/\n" +
                 "   file:" + basePath + "/META-INF/MANIFEST.MF\n" +
                 "  module:a\n" +
                 "  dir:" + dir.getPath() + "\n" +
                 " file:" + getLocalJarPath(jDomJar));
    assertManifest(null, jDomJar.getName());
  }

  private void assertManifest(final @Nullable String mainClass, final @Nullable String classpath) {
    if (myArtifact.getArtifactType() instanceof JarArtifactType) {
      ArtifactsTestUtil.assertManifest(myArtifact, getContext(), mainClass, classpath);
    }
    else {
      final CompositePackagingElement<?> archive = (CompositePackagingElement<?>)myArtifact.getRootElement().getChildren().get(0);
      ArtifactsTestUtil.assertManifest(archive, getContext(), myArtifact.getArtifactType(), mainClass, classpath);
    }
  }

  private void assertLayout(final String expected) {
    assertLayout(myArtifact, expected);
  }

  private void createFromTemplate(final @Nullable Module module, final @Nullable String mainClassName, final @Nullable String directoryForManifest,
                                  final boolean extractLibrariesToJar) {
    createFromTemplate(module, mainClassName, directoryForManifest, extractLibrariesToJar, false);
  }

  private void createFromTemplate(final Module module, final @Nullable String mainClassName, final @Nullable String directoryForManifest,
                                  final boolean extractLibrariesToJar, final boolean includeTests) {
    final JarFromModulesTemplate template = new JarFromModulesTemplate(getContext());
    final Module[] modules = module != null ? new Module[]{module} : ModuleManager.getInstance(getProject()).getModules();
    final ArtifactTemplate.NewArtifactConfiguration configuration =
      template.doCreateArtifact(modules, mainClassName, directoryForManifest, extractLibrariesToJar, includeTests);
    assertNotNull(configuration);
    myArtifact = addArtifact(configuration.getArtifactName(), configuration.getArtifactType(), configuration.getRootElement());
  }
}
