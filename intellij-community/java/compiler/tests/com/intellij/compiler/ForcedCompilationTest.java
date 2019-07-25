package com.intellij.compiler;

import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.io.File;
import java.io.IOException;

import static com.intellij.util.io.TestFileSystemBuilder.fs;

/**
 * @author nik
 */
public class ForcedCompilationTest extends BaseCompilerTestCase {

  public void testHandleDeletedFilesOnForcedCompilation() {
    VirtualFile a = createFile("dep/src/A.java", "class A{}");
    Module dep = addModule("dep", a.getParent());
    VirtualFile b = createFile("m/src/B.java", "class B {A a;}");
    Module m = addModule("m", b.getParent());
    ModuleRootModificationUtil.addDependency(m, dep);
    make(dep, m);
    assertOutput(dep, fs().file("A.class"));

    deleteFile(a);
    BuildManager.getInstance().clearState(myProject);
    recompile(dep);
    assertOutput(dep, fs());
    assertOutput(m, fs().file("B.class"));

    compile(getCompilerManager().createProjectCompileScope(myProject), false, true);
    assertOutput(dep, fs());
    assertOutput(m, fs());
  }

  public void testCleanOutputDirectoryOnRebuild() {
    Module m = addModule("m", createFile("src/A.java", "class A{}").getParent());
    make(m);
    createFileInOutput(m, "X.class");
    assertOutput(m, fs().file("A.class").file("X.class"));

    rebuild();
    assertOutput(m, fs().file("A.class"));
  }

  public void testRecompileResourceFileOnModuleRebuild() throws IOException {
    Module m = addModuleWithResourceRoot("m", createFile("res/a.xml", "a").getParent());
    addModule("empty", null);//need this to ensure that module rebuild won't be treated as project rebuild (JavaBuilderUtil.isForcedRecompilationAllJavaModules())
    make(m);
    assertOutput(m, fs().file("a.xml", "a"));
    assertModulesUpToDate();

    File outputDir = getOutputDir(m, false);
    FileUtil.writeToFile(new File(outputDir, "a.xml"), "b");
    assertModulesUpToDate();

    recompile(m);
    assertOutput(m, fs().file("a.xml", "a"));
  }

  public void testRecompileResourceFile() throws IOException {
    VirtualFile file = createFile("res/a.xml", "a");
    Module m = addModuleWithResourceRoot("m", file.getParent());
    make(m);
    assertOutput(m, fs().file("a.xml", "a"));
    assertModulesUpToDate();

    File outputDir = getOutputDir(m, false);
    FileUtil.writeToFile(new File(outputDir, "a.xml"), "b");
    assertModulesUpToDate();

    compile(true, file);
    assertOutput(m, fs().file("a.xml", "a"));
  }

  private Module addModuleWithResourceRoot(String name, VirtualFile resourceRoot) {
    Module m = addModule(name, null);
    PsiTestUtil.addContentRoot(m, resourceRoot);
    PsiTestUtil.addSourceRoot(m, resourceRoot, JavaResourceRootType.RESOURCE);
    return m;
  }
}
