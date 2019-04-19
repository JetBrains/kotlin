package com.intellij.compiler.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.artifacts.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author nik
 */
public class ArtifactUtilTest extends PackagingElementsTestCase {
  public void testProcessElementsWithRelativePath() {
    final Artifact a = addArtifact(root().dir("lib").file(createFile("a.txt")));
    final ElementToStringCollector processor = new ElementToStringCollector(true);
    ArtifactUtil.processElementsByRelativePath(a.getRootElement(), "lib/a.txt", getContext(), PlainArtifactType.getInstance(),
                                               PackagingElementPath.EMPTY, processor);
    assertEquals("/lib/file:" + getProjectBasePath() + "/a.txt\n", processor.getOutput());
  }

  public void testProcessDirectory() {
    final ElementToStringCollector processor = new ElementToStringCollector();
    final Artifact a = addArtifact(root().dir("lib").file(createFile("x.jar")));
    processDirectoryChildren(a.getRootElement(), "lib/", processor);
    assertEquals("file:" + getProjectBasePath() + "/x.jar\n", processor.getOutput());

    final Artifact b = addArtifact(root().artifact(a).dir("lib").file(createFile("y.jar")));
    processDirectoryChildren(b.getRootElement(), "lib", processor);
    assertEquals("file:" + getProjectBasePath() + "/x.jar\n" +
                 "file:" + getProjectBasePath() + "/y.jar\n", processor.getOutput());
  }

  public void testProcessParents() {
    final Artifact exploded = addArtifact("exploded:", root());
    final Artifact war = addArtifact("war", PlainArtifactType.getInstance(),
                                    archive("web.war")
                                        .dir("dir")
                                           .artifact(exploded)
                                        .build());
    addArtifact("ear", PlainArtifactType.getInstance(),
                archive("ear.ear")
                    .artifact(war)
                    .build());
    final MyParentElementProcessor processor = new MyParentElementProcessor();

    ArtifactUtil.processParents(exploded, getContext(), processor, 2);
    assertEquals("war:dir\n" +
                 "war:web.war/dir\n" +
                 "ear:ear.ear/web.war/dir\n", processor.getLog());

    ArtifactUtil.processParents(exploded, getContext(), processor, 1);
    assertEquals("war:dir\n" +
                 "war:web.war/dir\n", processor.getLog());

    ArtifactUtil.processParents(exploded, getContext(), processor, 0);
    assertEquals("war:dir\n", processor.getLog());

    ArtifactUtil.processParents(war, getContext(), processor, 2);
    assertEquals("war:web.war\n" +
                 "ear:ear.ear/web.war\n", processor.getLog());

  }

  public void testGetArtifactsContainingModuleOutput() {
    Module m = addModule("m", null);
    Artifact a = addArtifact("a", root().dir("d").module(m));
    Artifact b = addArtifact("b", root().artifact(a));
    addArtifact("c", root().file(createFile("x.txt")));
    assertSameElements(ArtifactUtil.getArtifactsContainingModuleOutput(m), a, b);
  }

  public void testCopyElement() {
    VirtualFile file = createFile("a.txt");
    final Artifact a = addArtifact(root().dir("lib").file(file));
    CompositePackagingElement<?> copy = ArtifactUtil.copyFromRoot(a.getRootElement(), myProject);
    assertLayout(copy, "<root>\n" +
                       " lib/\n" +
                       "  file:" + file.getPath() + "\n");
  }

  private void processDirectoryChildren(final CompositePackagingElement<?> rootElement, final String relativePath, ElementToStringCollector processor) {
    ArtifactUtil.processDirectoryChildren(rootElement, PackagingElementPath.EMPTY, relativePath, getContext(), PlainArtifactType.getInstance(), processor);
  }

  private static class ElementToStringCollector extends PackagingElementProcessor<PackagingElement<?>> {
    private final StringBuilder myBuilder = new StringBuilder();
    private final boolean myAddParentPaths;

    private ElementToStringCollector() {
      myAddParentPaths = false;
    }

    private ElementToStringCollector(boolean addParentPaths) {
      myAddParentPaths = addParentPaths;
    }

    @Override
    public boolean process(@NotNull PackagingElement<?> element, @NotNull PackagingElementPath path) {
      if (myAddParentPaths) {
        myBuilder.append(path.getPathString()).append("/");
      }
      myBuilder.append(element.toString()).append("\n");
      return true;
    }

    public String getOutput() {
      final String output = myBuilder.toString();
      myBuilder.setLength(0);
      return output;
    }
  }


  private static class MyParentElementProcessor extends ParentElementProcessor {
    private final StringBuilder myLog = new StringBuilder();

    @Override
    public boolean process(@NotNull CompositePackagingElement<?> element, @NotNull List<Pair<Artifact,CompositePackagingElement<?>>> parents, @NotNull Artifact artifact) {
      myLog.append(artifact.getName()).append(":").append(element.getName());
      for (Pair<Artifact, CompositePackagingElement<?>> parent : parents) {
        myLog.append("/").append(parent.getSecond().getName());
      }
      myLog.append("\n");
      return true;
    }

    public String getLog() {
      final String output = myLog.toString();
      myLog.setLength(0);
      return output;
    }
  }
}
