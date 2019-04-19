package com.intellij.compiler.artifacts.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import com.intellij.openapi.roots.ui.configuration.artifacts.actions.MovePackagingElementAction;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author nik
 */
public class MoveElementActionTest extends ArtifactEditorActionTestCase {
  public void testSimple() {
    createEditor(addArtifact(root()
                              .dir("a").end()
                              .dir("b")));
    assertDisabled();

    selectNode("b");
    assertDisabled();

    selectNode("a");
    perform();
    assertLayout("<root>\n" +
                 " b/\n" +
                 " a/");
  }

  public void testMoveIncludedArtifact() {
    final Artifact included = addArtifact("included", root().file(createFile("a.txt")));
    createEditor(addArtifact(root().artifact(included).dir("b")));

    selectNode("b");
    assertDisabled();

    selectNode("included");
    perform();
    assertLayout("<root>\n" +
                 " b/\n" +
                 " artifact:included");
  }

  public void testDoNotMoveInIncludedArtifact() {
    final Artifact included = addArtifact("included", root().file(createFile("a.txt")).file(createFile("b.txt")));
    createEditor(addArtifact(root().artifact(included)), true);
    selectNode("a.txt");
    assertDisabled();
  }

  public void testDoNotMixWithElementsFromIncludedArtifact() {
    final Artifact included = addArtifact("included", root().dir("dir").file(createFile("a.txt")));
    createEditor(addArtifact(root()
                              .dir("dir")
                                .file(createFile("a.txt"))
                                .file(createFile("b.txt"))
                                .end()
                              .artifact(included)), true);
    selectNode("dir");
    assertDisabled();
    selectNode("dir/a.txt");
    assertDisabled();
    selectNode("dir/b.txt");
    assertDisabled();
  }

  @Override
  protected AnAction createAction(ArtifactEditorEx artifactEditor) {
    return new MovePackagingElementAction(artifactEditor.getLayoutTreeComponent(), "", "", null, 1);
  }
}
