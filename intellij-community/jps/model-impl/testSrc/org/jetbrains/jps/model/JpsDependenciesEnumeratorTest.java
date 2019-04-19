/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model;

import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.project.IntelliJProjectConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.*;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsLibraryDependency;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.jetbrains.jps.model.java.JpsJavaExtensionService.dependencies;


/**
 * @author nik
 */
public class JpsDependenciesEnumeratorTest extends JpsJavaModelTestCase {
  private JpsModule myModule;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    JpsTypedLibrary<JpsSdk<JpsDummyElement>> jdk = addJdk("1.7");
    myModule = addModule();
    JpsModuleRootModificationUtil.setModuleSdk(myModule, jdk.getProperties());
  }

  @NotNull
  private JpsTypedLibrary<JpsSdk<JpsDummyElement>> addJdk(final String mockJdkVersion) {
    final String mockJdkDir = "mockJDK-" + mockJdkVersion;
    File home = PathManagerEx.findFileUnderCommunityHome("java/" + mockJdkDir);
    JpsTypedLibrary<JpsSdk<JpsDummyElement>> jdk = myModel.getGlobal().addSdk(mockJdkVersion, home.getAbsolutePath(), mockJdkVersion, JpsJavaSdkType.INSTANCE);
    jdk.addRoot(getRtJar(mockJdkDir), JpsOrderRootType.COMPILED);
    return jdk;
  }

  public void testLibrary() {
    JpsModuleRootModificationUtil.addDependency(myModule, createJDomLibrary());

    assertClassRoots(dependencies(myModule), getRtJarJdk17(), getJDomJar());
    assertClassRoots(dependencies(myModule).withoutSdk(), getJDomJar());
    assertClassRoots(dependencies(myModule).withoutSdk().productionOnly().runtimeOnly(), getJDomJar());
    assertClassRoots(dependencies(myModule).withoutLibraries(), getRtJarJdk17());
    assertSourceRoots(dependencies(myModule), getJDomSources());
  }

  private static String getJDomSources() {
    //todo[nik] download sources of JDOM library and locate the JAR via IntelliJProjectConfiguration instead
    return JpsPathUtil.getLibraryRootUrl(PathManagerEx.findFileUnderCommunityHome("lib/src/jdom.zip"));
  }

  private static String getJDomJar() {
    return getJarUrlFromProjectLib("JDOM");
  }

  private static String getAsmJar() {
    return getJarUrlFromProjectLib("ASM");
  }

  private static String getJarUrlFromProjectLib(final String libraryName) {
    return assertOneElement(IntelliJProjectConfiguration.getProjectLibraryClassesRootUrls(libraryName));
  }

  private static String getRtJarJdk17() {
    return getRtJar("mockJDK-1.7");
  }

  private static String getRtJarJdk18() {
    return getRtJar("mockJDK-1.8");
  }

  private static String getRtJar(final String mockJdkDir) {
    return JpsPathUtil.getLibraryRootUrl(PathManagerEx.findFileUnderCommunityHome("java/" + mockJdkDir + "/jre/lib/rt.jar"));
  }

  private JpsLibrary createJDomLibrary() {
    JpsLibrary library = addLibrary("jdom");
    library.addRoot(getJDomJar(), JpsOrderRootType.COMPILED);
    library.addRoot(getJDomSources(), JpsOrderRootType.SOURCES);
    return library;
  }

  private JpsLibrary createAsmLibrary() {
    JpsLibrary library = addLibrary("asm");
    library.addRoot(getAsmJar(), JpsOrderRootType.COMPILED);
    return library;
  }

  public void testModuleSources() {
    final String srcRoot = addSourceRoot(myModule, false);
    final String testRoot = addSourceRoot(myModule, true);
    final String output = setModuleOutput(myModule, false);
    final String testOutput = setModuleOutput(myModule, true);

    assertClassRoots(dependencies(myModule).withoutSdk(), testOutput, output);
    assertClassRoots(dependencies(myModule).withoutSdk().productionOnly(), output);
    assertSourceRoots(dependencies(myModule), srcRoot, testRoot);
    assertSourceRoots(dependencies(myModule).productionOnly(), srcRoot);

    assertEnumeratorRoots(dependencies(myModule).withoutSdk().classes().withoutSelfModuleOutput(), output);
    assertEnumeratorRoots(dependencies(myModule).withoutSdk().productionOnly().classes().withoutSelfModuleOutput());
  }

  public void testLibraryScope() {
    JpsLibraryDependency dependency = myModule.getDependenciesList().addLibraryDependency(createJDomLibrary());
    getJavaService().getOrCreateDependencyExtension(dependency).setScope(JpsJavaDependencyScope.RUNTIME);
    JpsModuleRootModificationUtil.addDependency(myModule, createJDomLibrary(), JpsJavaDependencyScope.RUNTIME, false);

    assertClassRoots(dependencies(myModule).withoutSdk(), getJDomJar());
    assertClassRoots(dependencies(myModule).withoutSdk().exportedOnly());
    assertClassRoots(dependencies(myModule).withoutSdk().compileOnly());
  }

  public void testModuleDependency() {
    final JpsModule dep = addModule("dep");
    final String depSrcRoot = addSourceRoot(dep, false);
    final String depTestRoot = addSourceRoot(dep, true);
    final String depOutput = setModuleOutput(dep, false);
    final String depTestOutput = setModuleOutput(dep, true);
    JpsModuleRootModificationUtil.addDependency(dep, createJDomLibrary(), JpsJavaDependencyScope.COMPILE, true);
    JpsModuleRootModificationUtil.addDependency(myModule, dep, JpsJavaDependencyScope.COMPILE, true);

    final String srcRoot = addSourceRoot(myModule, false);
    final String testRoot = addSourceRoot(myModule, true);
    final String output = setModuleOutput(myModule, false);
    final String testOutput = setModuleOutput(myModule, true);

    assertClassRoots(dependencies(myModule).withoutSdk(), testOutput, output, depTestOutput, depOutput);
    assertClassRoots(dependencies(myModule).withoutSdk().recursively(), testOutput, output, depTestOutput, depOutput, getJDomJar());
    assertSourceRoots(dependencies(myModule), srcRoot, testRoot, depSrcRoot, depTestRoot);
    assertSourceRoots(dependencies(myModule).recursively(), srcRoot, testRoot, depSrcRoot, depTestRoot, getJDomSources());

    assertClassRoots(dependencies(myModule).withoutSdk().withoutModuleSourceEntries().recursively(), getJDomJar());
    assertSourceRoots(dependencies(myModule).withoutSdk().withoutModuleSourceEntries().recursively(), getJDomSources());
    assertEnumeratorRoots(dependencies(myModule).withoutSdk().withoutModuleSourceEntries().recursively().classes(), getJDomJar());
    assertEnumeratorRoots(dependencies(myModule).withoutSdk().withoutModuleSourceEntries().recursively().sources(), getJDomSources());

    assertEnumeratorRoots(dependencies(myModule).withoutSdk().recursively().classes().withoutSelfModuleOutput(),
                          output, depTestOutput, depOutput, getJDomJar());
    assertEnumeratorRoots(dependencies(myModule).productionOnly().withoutSdk().recursively().classes().withoutSelfModuleOutput(),
                          depOutput, getJDomJar());

    assertClassRoots(dependencies(myModule).withoutSdk().withoutDepModules().withoutModuleSourceEntries().recursively(), getJDomJar());
    assertEnumeratorRoots(
      dependencies(myModule).productionOnly().withoutSdk().withoutDepModules().withoutModuleSourceEntries().recursively().classes(),
      getJDomJar());
    assertClassRoots(dependencies(myModule).withoutSdk().withoutDepModules().withoutModuleSourceEntries());
    assertEnumeratorRoots(dependencies(myModule).productionOnly().withoutModuleSourceEntries().withoutSdk().withoutDepModules().classes());
  }

  public void testModuleJpsJavaDependencyScope() {
    final JpsModule dep = addModule("dep");
    JpsModuleRootModificationUtil.addDependency(dep, createJDomLibrary(), JpsJavaDependencyScope.COMPILE, true);
    JpsModuleRootModificationUtil.addDependency(myModule, dep, JpsJavaDependencyScope.TEST, true);

    assertClassRoots(dependencies(myModule).withoutSdk());
    assertClassRoots(dependencies(myModule).withoutSdk().recursively(), getJDomJar());
    assertClassRoots(dependencies(myModule).withoutSdk().exportedOnly().recursively(), getJDomJar());
    assertClassRoots(dependencies(myModule).withoutSdk().productionOnly().recursively());

    assertClassRoots(dependencies(myProject).withoutSdk(), getJDomJar());
    assertClassRoots(dependencies(myProject).withoutSdk().productionOnly(), getJDomJar());
  }

  public void testNotExportedLibrary() {
    final JpsModule dep = addModule("dep");
    JpsModuleRootModificationUtil.addDependency(dep, createJDomLibrary(), JpsJavaDependencyScope.COMPILE, false);
    JpsModuleRootModificationUtil.addDependency(myModule, createAsmLibrary(), JpsJavaDependencyScope.COMPILE, false);
    JpsModuleRootModificationUtil.addDependency(myModule, dep, JpsJavaDependencyScope.COMPILE, false);

    assertClassRoots(dependencies(myModule).withoutSdk(), getAsmJar());
    assertClassRoots(dependencies(myModule).withoutSdk().recursively(), getAsmJar(), getJDomJar());
    assertClassRoots(dependencies(myModule).withoutSdk().recursivelyExportedOnly(), getAsmJar());
    assertClassRoots(dependencies(myModule).withoutSdk().exportedOnly().recursively());
  }

  public void testAnnotations() {
    JpsLibrary library = addLibrary();
    String libraryUrl = "temp:///library";
    library.addRoot(libraryUrl, JpsAnnotationRootType.INSTANCE);
    JpsModuleRootModificationUtil.addDependency(myModule, library);
    assertEnumeratorRoots(dependencies(myModule).annotations(), libraryUrl);

    String moduleUrl = "temp://module";
    JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(myModule).getAnnotationRoots().addUrl(moduleUrl);
    assertEnumeratorRoots(dependencies(myModule).annotations(), moduleUrl, libraryUrl);
  }

  public void testJdkIsNotExported() {
    assertClassRoots(dependencies(myModule).exportedOnly());
  }

  public void testDoNotAddJdkRootsFromModuleDependency() {
    JpsModule dep = addModule("dep");
    JpsTypedLibrary<JpsSdk<JpsDummyElement>> jdk8 = addJdk("1.8");
    JpsModuleRootModificationUtil.addDependency(myModule, dep);
    JpsModuleRootModificationUtil.setModuleSdk(dep, jdk8.getProperties());
    assertClassRoots(dependencies(myModule).recursively(), getRtJarJdk17());
    assertClassRoots(dependencies(dep).recursively(), getRtJarJdk18());
  }

  public void testProject() {
    JpsModuleRootModificationUtil.addDependency(myModule, createJDomLibrary());

    final String srcRoot = addSourceRoot(myModule, false);
    final String testRoot = addSourceRoot(myModule, true);
    final String output = setModuleOutput(myModule, false);
    final String testOutput = setModuleOutput(myModule, true);

    assertClassRoots(dependencies(myProject).withoutSdk(), testOutput, output, getJDomJar());
    assertSourceRoots(dependencies(myProject).withoutSdk(), srcRoot, testRoot, getJDomSources());
  }

  public void testModules() {
    JpsModuleRootModificationUtil.addDependency(myModule, createJDomLibrary());

    final String srcRoot = addSourceRoot(myModule, false);
    final String testRoot = addSourceRoot(myModule, true);
    final String output = setModuleOutput(myModule, false);
    final String testOutput = setModuleOutput(myModule, true);

    assertClassRoots(getJavaService().enumerateDependencies(Collections.singletonList(myModule)).withoutSdk(),
                     testOutput, output, getJDomJar());
    assertSourceRoots(getJavaService().enumerateDependencies(Collections.singletonList(myModule)).withoutSdk(),
                      srcRoot, testRoot, getJDomSources());
  }

  private String setModuleOutput(JpsModule module, boolean tests) {
    try {
      File file = FileUtil.createTempDirectory(module.getName(), tests ? "testSrc" : "src");
      JpsJavaModuleExtension extension = getJavaService().getOrCreateModuleExtension(module);
      String url = JpsPathUtil.getLibraryRootUrl(file);
      if (tests) {
        extension.setTestOutputUrl(url);
      }
      else {
        extension.setOutputUrl(url);
      }
      return url;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String addSourceRoot(JpsModule module, boolean tests) {
    try {
      File file = FileUtil.createTempDirectory(module.getName(), tests ? "testSrc" : "src");
      return module.addSourceRoot(JpsPathUtil.getLibraryRootUrl(file), tests ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE).getUrl();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void assertClassRoots(final JpsJavaDependenciesEnumerator enumerator, String... urls) {
    assertEnumeratorRoots(enumerator.classes(), urls);
  }

  private static void assertSourceRoots(final JpsJavaDependenciesEnumerator enumerator, String... urls) {
    assertEnumeratorRoots(enumerator.sources(), urls);
  }

  private static void assertEnumeratorRoots(JpsJavaDependenciesRootsEnumerator rootsEnumerator, String... urls) {
    assertOrderedEquals(rootsEnumerator.getUrls(), urls);
  }
}
