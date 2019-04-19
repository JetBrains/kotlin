package com.intellij.compiler.artifacts.ui;

import com.intellij.compiler.artifacts.PackagingElementsTestCase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ui.configuration.artifacts.*;
import com.intellij.openapi.roots.ui.configuration.artifacts.nodes.ComplexPackagingElementNode;
import com.intellij.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.Ref;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.util.PathUtil;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author nik
 */
public abstract class ArtifactEditorTestCase extends PackagingElementsTestCase {
  protected ArtifactEditorImpl myArtifactEditor;

  @Override
  protected void tearDown() throws Exception {
    myArtifactEditor = null;
    super.tearDown();
  }

  protected void createEditor(Artifact artifact) {
    createEditor(artifact, false);
  }

  protected void showContent(String path) {
    selectNode(path);
    final PackagingElementNode<?> node = myArtifactEditor.getLayoutTreeComponent().getSelection().getNodeIfSingle();
    myArtifactEditor.getSubstitutionParameters().setShowContent(assertInstanceOf(node, ComplexPackagingElementNode.class));
    myArtifactEditor.rebuildTries();
  }

  protected void createEditor(Artifact artifact, final boolean showContent) {
    final ArtifactEditorSettings settings;
    if (showContent) {
      settings = new ArtifactEditorSettings(false, Arrays.asList(PackagingElementFactory.getInstance().getComplexElementTypes()));
    }
    else {
      settings = new ArtifactEditorSettings(false, Collections.emptyList());
    }

    myArtifactEditor = new ArtifactEditorImpl(new MockArtifactsStructureConfigurableContext(), artifact, settings) {
      @Override
      protected ArtifactEditorContextImpl createArtifactEditorContext(ArtifactsStructureConfigurableContext parentContext) {
        return new MockPackagingEditorContext(parentContext, this);
      }
    };
    myArtifactEditor.createMainComponent();
    disposeOnTearDown(myArtifactEditor);
  }

  protected void selectNode(String path) {
    final LayoutTreeComponent layoutTreeComponent = myArtifactEditor.getLayoutTreeComponent();
    layoutTreeComponent.getLayoutTree().clearSelection();
    layoutTreeComponent.selectNode(PathUtil.getParentPath(path), PathUtil.getFileName(path));
    assertFalse("Node " + path + " not found", layoutTreeComponent.getSelection().getNodes().isEmpty());
  }

  protected void assertLayout(String expected) {
    assertLayout(myArtifactEditor.getArtifact(), expected);
  }

  protected void applyChanges() {
    ApplicationManager.getApplication().runWriteAction(() -> {
      myArtifactEditor.apply();
      ((MockArtifactsStructureConfigurableContext)myArtifactEditor.getContext().getParent()).commitModel();
    });
  }

  protected static void runAction(final Runnable action, boolean confirmationExpected) {
    final Ref<Boolean> dialogShown = Ref.create(false);
    final TestDialog oldDialog = Messages.setTestDialog(new TestDialog() {
      @Override
      public int show(String message) {
        dialogShown.set(true);
        return 0;
      }
    });

    try {
      action.run();
    }
    finally {
      Messages.setTestDialog(oldDialog);
    }

    if (confirmationExpected) {
      assertTrue("Dialog wasn't shown", dialogShown.get());
    }
    else {
      assertFalse("Dialog was shown", dialogShown.get());
    }
  }
}
