// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.intellij.util.io.TestFileSystemBuilder.fs;

/**
 * @author nik
 */
public class UnloadedModulesCompilationTest extends BaseCompilerTestCase {
  public void testDoNotCompileUnloadedModulesByDefault() {
    VirtualFile a = createFile("unloaded/src/A.java", "class A{ error }");
    Module unloaded = addModule("unloaded", a.getParent());
    List<String> unloadedList = Collections.singletonList(unloaded.getName());
    ModuleManager.getInstance(myProject).setUnloadedModules(unloadedList);
    buildAllModules().assertUpToDate();
  }

  public void testCompileUnloadedModulesIfExplicitlySpecified() {
    VirtualFile a = createFile("unloaded/src/A.java", "class A{}");
    Module unloaded = addModule("unloaded", a.getParent());
    File outputDir = getOutputDir(unloaded, false);

    List<String> unloadedList = Collections.singletonList(unloaded.getName());
    ModuleManager.getInstance(myProject).setUnloadedModules(unloadedList);

    make(createScopeWithUnloaded(Collections.emptyList(), unloadedList));
    fs().file("A.class").build().assertDirectoryEqual(outputDir);
  }

  public void testCompileUsagesOfConstantInUnloadedModules() throws IOException {
    VirtualFile utilFile = createFile("unloaded/src/Util.java", "class Util { public static final String FOO = \"foo\"; }");
    VirtualFile a = createFile("unloaded/src/A.java", "class A{ { System.out.println(Util.FOO); } }");
    Module unloaded = addModule("unloaded", a.getParent());
    buildAllModules();

    List<String> unloadedList = Collections.singletonList(unloaded.getName());
    ModuleManager.getInstance(myProject).setUnloadedModules(unloadedList);

    changeFile(utilFile, VfsUtilCore.loadText(utilFile).replace("foo", "foo2"));

    make(createScopeWithUnloaded(Collections.emptyList(), unloadedList)).assertGenerated("A.class", "Util.class");
  }

  public void testCompileUsagesOfConstantFromNormalModuleInInUnloadedModules() throws IOException {
    VirtualFile utilFile = createFile("util/src/Util.java", "class Util { public static final String FOO = \"foo\"; }");
    Module util = addModule("util", utilFile.getParent());
    VirtualFile a = createFile("unloaded/src/A.java", "class A{ { System.out.println(Util.FOO); } }");
    Module unloaded = addModule("unloaded", a.getParent());
    ModuleRootModificationUtil.addDependency(unloaded, util);
    buildAllModules();

    List<String> unloadedList = Collections.singletonList(unloaded.getName());
    ModuleManager.getInstance(myProject).setUnloadedModules(unloadedList);

    changeFile(utilFile, VfsUtilCore.loadText(utilFile).replace("foo", "foo2"));

    make(createScopeWithUnloaded(Collections.singletonList(util), unloadedList)).assertGenerated("A.class", "Util.class");
  }

  public void testCompileUnloadedModuleAfterBuildingAllLoadedModules() {
    VirtualFile utilFile = createFile("util/src/Util.java", "class Util { }");
    Module util = addModule("util", utilFile.getParent());
    VirtualFile a = createFile("unloaded/src/A.java", "class A { Util u = new Util();  }");
    Module unloaded = addModule("unloaded", a.getParent());
    ModuleRootModificationUtil.addDependency(unloaded, util);
    buildAllModules();

    List<String> unloadedList = Collections.singletonList(unloaded.getName());
    ModuleManager.getInstance(myProject).setUnloadedModules(unloadedList);

    changeFile(utilFile, "class Util { Util(int i) {} }");
    buildAllModules().assertGenerated("Util.class");

    compile(createScopeWithUnloaded(Collections.singletonList(util), unloadedList), false, true);
  }

  @NotNull
  private ModuleCompileScope createScopeWithUnloaded(List<Module> modules, List<String> unloaded) {
    return new ModuleCompileScope(myProject, modules, unloaded, true, false);
  }
}
