package com.intellij.compiler.artifacts.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import com.intellij.openapi.roots.ui.configuration.artifacts.actions.InlineArtifactAction;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author nik
 */
public class InlineArtifactActionTest extends ArtifactEditorActionTestCase {
  private Artifact myIncluded;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    final Module module = addModule("mod", null);
    myIncluded = addArtifact("included", root().module(module));
  }

  @Override
  protected void tearDown() throws Exception {
    myIncluded = null;
    super.tearDown();
  }

  public void testSimple() {
    createEditor(addArtifact(root().artifact(myIncluded)));
    selectNode("included");
    perform();
    assertLayout("<root>\n" +
                 " module:mod");
  }

  public void testDisabledForIncluded() {
    final Artifact a = addArtifact("a", root().artifact(myIncluded));
    createEditor(addArtifact(root().artifact(a)));
    showContent("a");
    selectNode("included");
    assertWillNotBePerformed();
  }

  public void testInlineIntoDirectory() {
    final Artifact a = addArtifact("a", root().dir("dir").file("a.txt"));
    createEditor(addArtifact(root()
                              .artifact(a)
                              .dir("dir").artifact(myIncluded)));
    selectNode("dir");
    assertDisabled();

    selectNode("dir/included");
    perform();
    assertLayout("<root>\n" +
                 " artifact:a\n" +
                 " dir/\n" +
                 "  module:mod");
  }

  @Override
  protected AnAction createAction(ArtifactEditorEx artifactEditor) {
    return new InlineArtifactAction(artifactEditor);
  }
}
