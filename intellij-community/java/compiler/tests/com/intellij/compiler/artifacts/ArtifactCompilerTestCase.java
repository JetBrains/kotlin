package com.intellij.compiler.artifacts;

import com.intellij.compiler.BaseCompilerTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.util.io.TestFileSystemBuilder;
import com.intellij.util.io.TestFileSystemItem;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.intellij.compiler.artifacts.ArtifactsTestCase.commitModel;

/**
 * @author nik
 */
public abstract class ArtifactCompilerTestCase extends BaseCompilerTestCase {

  protected void deleteArtifact(final Artifact artifact) {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.removeArtifact(artifact);
    commitModel(model);
  }

  protected Artifact addArtifact(TestPackagingElementBuilder builder) {
    return addArtifact("a", builder);
  }

  protected Artifact addArtifact(final String name, TestPackagingElementBuilder builder) {
    return addArtifact(name, builder.build());
  }

  protected Artifact addArtifact(String name, final CompositePackagingElement<?> root) {
    return addArtifact(name, PlainArtifactType.getInstance(), root);
  }

  protected Artifact addArtifact(final String name, final ArtifactType type, final CompositePackagingElement<?> root) {
    return getArtifactManager().addArtifact(name, type, root);
  }

  protected Library addProjectLibrary(final @Nullable Module module, final String name, final VirtualFile... jars) {
    return addProjectLibrary(module, name, DependencyScope.COMPILE, jars);
  }

  protected Library addProjectLibrary(final @Nullable Module module, final String name, final DependencyScope scope,
                                      final VirtualFile... jars) {
    return PackagingElementsTestCase.addProjectLibrary(myProject, module, name, scope, jars);
  }

  protected TestPackagingElementBuilder root() {
    return TestPackagingElementBuilder.root(myProject);
  }

  protected TestPackagingElementBuilder archive(String name) {
    return TestPackagingElementBuilder.archive(myProject, name);
  }

  protected CompilationLog compileProject() {
    return make(getArtifactManager().getArtifacts());
  }

  protected void changeFileInJar(String jarPath, String pathInJar) {
    final VirtualFile jarFile = LocalFileSystem.getInstance().findFileByPath(jarPath);
    assertNotNull(jarFile);
    final VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(jarFile);
    assertNotNull(jarRoot);
    VirtualFile jarEntry = jarRoot.findFileByRelativePath(pathInJar);
    assertNotNull(jarEntry);
    assertNotNull(jarFile);
    changeFile(jarFile);
    jarFile.refresh(false, false);

    jarEntry = jarRoot.findFileByRelativePath(pathInJar);
    assertNotNull(jarEntry);
  }

  protected static TestFileSystemBuilder fs() {
    return TestFileSystemItem.fs();
  }

  public static void assertNoOutput(Artifact artifact) {
    final String outputPath = artifact.getOutputPath();
    assertNotNull("output path not specified for " + artifact.getName(), outputPath);
    assertFalse(new File(FileUtil.toSystemDependentName(outputPath)).exists());
  }

  public static void assertEmptyOutput(Artifact a1) {
    assertOutput(a1, ArtifactCompilerTestCase.fs());
  }

  public static void assertOutput(Artifact artifact, TestFileSystemBuilder item) {
    final VirtualFile outputFile = getOutputDir(artifact);
    outputFile.refresh(false, true);
    item.build().assertDirectoryEqual(VfsUtil.virtualToIoFile(outputFile));
  }

  protected static VirtualFile getOutputDir(Artifact artifact) {
    final String output = artifact.getOutputPath();
    assertNotNull("output path not specified for " + artifact.getName(), output);
    final VirtualFile outputFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(output);
    assertNotNull("output file not found " + output, outputFile);
    return outputFile;
  }
}
