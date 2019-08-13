package com.intellij.compiler.artifacts.ui;

import com.intellij.compiler.artifacts.ArtifactsTestUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import com.intellij.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import com.intellij.openapi.roots.ui.configuration.artifacts.actions.ExtractArtifactAction;
import com.intellij.openapi.roots.ui.configuration.artifacts.actions.IExtractArtifactDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;

/**
 * @author nik
 */
public class ExtractArtifactActionTest extends ArtifactEditorActionTestCase {

  public void testSimple() {
    createEditor(addArtifact(root().file(createFile("a.txt"))));
    assertDisabled();

    selectNode("a.txt");
    perform();
    assertLayout("<root>\n" +
                 " artifact:a_txt");

    applyChanges();
    assertLayout(ArtifactsTestUtil.findArtifact(myProject, "a_txt"), "<root>\n" +
                                                                   " file:" + getProjectBasePath() + "/a.txt");
  }

  public void testDisabledForJarFromLib() {
    final VirtualFile jar = getJDomJar();
    createEditor(addArtifact(root().lib(addProjectLibrary(null, "dom", jar))), true);

    selectNode(jar.getName());
    assertWillNotBePerformed();
  }

  public void testDirectoryFromIncludedArtifact() {
    final Artifact included = addArtifact("included", root().dir("dir").file(createFile("a.txt")));
    createEditor(addArtifact(root()
                              .artifact(included)
                              .dir("dir")
                                .file(createFile("b.txt"))), true);
    selectNode("dir");
    assertDisabled();

    selectNode("dir/a.txt");
    assertWillNotBePerformed();

    selectNode("dir/b.txt");
    perform();
    assertLayout("<root>\n" +
                 " artifact:included\n" +
                 " dir/\n" +
                 "  artifact:b_txt");
    applyChanges();
    assertLayout(ArtifactsTestUtil.findArtifact(myProject, "b_txt"), "<root>\n" +
                                                                   " file:" + getProjectBasePath() + "/b.txt");
  }

  @Override
  protected AnAction createAction(final ArtifactEditorEx artifactEditor) {
    return new ExtractArtifactAction(artifactEditor) {
      @Override
      protected IExtractArtifactDialog showDialog(LayoutTreeComponent treeComponent, final String initialName) {
        return new IExtractArtifactDialog() {
          @Override
          public String getArtifactName() {
            return initialName;
          }

          @Override
          public ArtifactType getArtifactType() {
            return PlainArtifactType.getInstance();
          }
        };
      }
    };
  }
}
