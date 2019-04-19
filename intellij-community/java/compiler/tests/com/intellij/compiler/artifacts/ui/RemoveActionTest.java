package com.intellij.compiler.artifacts.ui;

import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author nik
 */
public class RemoveActionTest extends ArtifactEditorTestCase {

  public void testSimple() {
    createEditor(addArtifact(root().file(createFile("a.txt"))));

    assertLayout("<root>\n" +
                 " file:" + getProjectBasePath() + "/a.txt");
    selectNode("a.txt");
    removeSelected(false);

    assertLayout("<root>");
  }

  public void testElementInIncludedArtifact() {
    final Artifact included = addArtifact("included", root().file(createFile("a.txt")));
    createEditor(addArtifact(archive("x.jar").artifact(included)), true);
    selectNode("a.txt");

    removeSelected(true);

    assertLayout("x.jar");
    applyChanges();
    assertLayout(included, "<root>\n" +
                           " file:" + getProjectBasePath() + "/a.txt");
  }

  public void testElementInDirInIncludedArtifact() {
    final Artifact included = addArtifact("included", root().dir("y").file(createFile("z.txt")));
    createEditor(addArtifact(root().dir("x").artifact(included)), true);

    selectNode("x/y/z.txt");
    removeSelected(true);

    assertLayout("<root>\n" +
                 " x/\n");
  }

  public void testJarFileInLibrary() {
    VirtualFile jDomJar = getJDomJar();
    final Library library = addProjectLibrary(null, "jdom", jDomJar);
    createEditor(addArtifact(root().lib(library)), true);

    selectNode(jDomJar.getName());
    removeSelected(true);

    assertLayout("<root>");
  }

  public void testOneFileInThreeArtifacts() {
    final VirtualFile file = createFile("a.txt");
    final Artifact a1 = addArtifact("a1", root().file(file));
    final Artifact a2 = addArtifact("a2", root().file(file));
    createEditor(addArtifact(root().artifact(a1).artifact(a2).file(file)), true);

    selectNode("a.txt");

    assertLayout("<root>\n" +
                 " artifact:a1\n" +
                 " artifact:a2\n" +
                 " file:" + getProjectBasePath() + "/a.txt");
    removeSelected(true);
    assertLayout("<root>");
  }

  public void testDirectoryFromTwoArtifacts() {
    final Artifact a1 = addArtifact("a1", root().dir("dir").file(createFile("a.txt")));
    final Artifact a2 = addArtifact("a2", root().dir("dir").file(createFile("b.txt")));
    createEditor(addArtifact(root().dir("x").artifact(a1).artifact(a2)), true);

    selectNode("x/dir");

    assertLayout("<root>\n" +
                 " x/\n" +
                 "  artifact:a1\n" +
                 "  artifact:a2");
    removeSelected(true);
    assertLayout("<root>\n" +
                 " x/");
  }

  public void testArtifactIncludedInArtifact() {
    final Artifact a1 = addArtifact("a1", root().file(createFile("a.txt")));
    final Artifact a2 = addArtifact("a2", root().artifact(a1));
    createEditor(addArtifact(root().artifact(a2)), true);

    selectNode("a.txt");

    assertLayout("<root>\n" +
                 " artifact:a2");
    removeSelected(true);
    assertLayout("<root>");
  }

  private void removeSelected(boolean confirmationExpected) {
    runAction(() -> myArtifactEditor.getLayoutTreeComponent().removeSelectedElements(), confirmationExpected);
  }

}
