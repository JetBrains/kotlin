package com.intellij.compiler.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;

/**
* @author nik
*/
public class TestPackagingElementBuilder {
  private final CompositePackagingElement<?> myElement;
  private final TestPackagingElementBuilder myParent;
  private final Project myProject;

  private TestPackagingElementBuilder(Project project, CompositePackagingElement<?> element, TestPackagingElementBuilder parent) {
    myElement = element;
    myParent = parent;
    myProject = project;
  }

  public static TestPackagingElementBuilder root(Project project) {
    return new TestPackagingElementBuilder(project, PackagingElementFactory.getInstance().createArtifactRootElement(), null);
  }

  public static TestPackagingElementBuilder archive(final Project project, String name) {
    return new TestPackagingElementBuilder(project, PackagingElementFactory.getInstance().createArchive(name), null);
  }

  public CompositePackagingElement<?> build() {
    TestPackagingElementBuilder builder = this;
    while (builder.myParent != null) {
      builder = builder.myParent;
    }
    return builder.myElement;
  }

  public TestPackagingElementBuilder file(VirtualFile file) {
    return file(file.getPath());
  }

  public TestPackagingElementBuilder file(String path) {
    myElement.addOrFindChild(getFactory().createFileCopyWithParentDirectories(path, "/"));
    return this;
  }

  private static PackagingElementFactory getFactory() {
    return PackagingElementFactory.getInstance();
  }

  public TestPackagingElementBuilder dirCopy(VirtualFile dir) {
    return dirCopy(dir.getPath());
  }

  public TestPackagingElementBuilder dirCopy(String path) {
    myElement.addOrFindChild(getFactory().createDirectoryCopyWithParentDirectories(path, "/"));
    return this;
  }

  public TestPackagingElementBuilder extractedDir(String jarPath, String pathInJar) {
    myElement.addOrFindChild(getFactory().createExtractedDirectoryWithParentDirectories(jarPath, pathInJar, "/"));
    return this;
  }

  public TestPackagingElementBuilder module(Module module) {
    myElement.addOrFindChild(getFactory().createModuleOutput(module));
    return this;
  }

  public TestPackagingElementBuilder moduleSource(Module module) {
    myElement.addOrFindChild(getFactory().createModuleSource(module));
    return this;
  }

  public TestPackagingElementBuilder lib(Library library) {
    myElement.addOrFindChildren(getFactory().createLibraryElements(library));
    return this;
  }

  public TestPackagingElementBuilder artifact(Artifact artifact) {
    myElement.addOrFindChild(getFactory().createArtifactElement(artifact, myProject));
    return this;
  }

  public TestPackagingElementBuilder archive(String name) {
    final CompositePackagingElement<?> archive = getFactory().createArchive(name);
    return new TestPackagingElementBuilder(myProject, myElement.addOrFindChild(archive), this);
  }

  public TestPackagingElementBuilder dir(String name) {
    return new TestPackagingElementBuilder(myProject, myElement.addOrFindChild(getFactory().createDirectory(name)), this);
  }

  public TestPackagingElementBuilder add(PackagingElement<?> element) {
    myElement.addOrFindChild(element);
    return this;
  }

  public TestPackagingElementBuilder end() {
    return myParent;
  }
}
