package com.intellij.compiler.artifacts.ui;

import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.elements.ProductionModuleOutputElementType;
import com.intellij.packaging.impl.elements.ProductionModuleSourceElementType;

/**
 * @author nik
 */
public class AddNewElementActionTest extends ArtifactEditorTestCase {

  public void testSimple() {
    addModule();
    createEditor(addArtifact(root()));
    myArtifactEditor.addNewPackagingElement(ProductionModuleOutputElementType.ELEMENT_TYPE);
    assertLayout("<root>\n" +
                 " module:mod");
  }

  public void testAddToDirectory() {
    addModule();
    createEditor(addArtifact(root().dir("dir")));
    selectNode("dir");
    myArtifactEditor.addNewPackagingElement(ProductionModuleOutputElementType.ELEMENT_TYPE);
    assertLayout("<root>\n" +
                 " dir/\n" +
                 "  module:mod");
  }

  public void testAddSourcesToDirectory() {
    addModule();
    createEditor(addArtifact(root().dir("dir")));
    selectNode("dir");
    myArtifactEditor.addNewPackagingElement(ProductionModuleSourceElementType.ELEMENT_TYPE);
    assertLayout("<root>\n" +
                 " dir/\n" +
                 "  module sources:mod");
  }

  public void testAddToDirectoryInIncludedArtifact() {
    addModule();
    Artifact included = addArtifact("included", root().dir("dir"));
    createEditor(addArtifact(root().artifact(included)), true);
    selectNode("dir");
    myArtifactEditor.addNewPackagingElement(ProductionModuleOutputElementType.ELEMENT_TYPE);
    assertLayout("<root>\n" +
                 " artifact:included\n" +
                 " dir/\n" +
                 "  module:mod");
  }

  public void testDoNotAddIntoIncludedArchive() {
    addModule();
    final Artifact included = addArtifact("included", root().archive("x.jar"));
    createEditor(addArtifact(root().artifact(included)), true);
    selectNode("x.jar");
    addNewElement(ProductionModuleOutputElementType.ELEMENT_TYPE, true);
    assertLayout("<root>\n" +
                 " artifact:included");
  }

  private void addModule() {
    addModule("mod", createDir("src"));
  }

  private void addNewElement(final ProductionModuleOutputElementType elementType, final boolean confirmationExpected) {
    runAction(() -> myArtifactEditor.addNewPackagingElement(elementType), confirmationExpected);
  }
}
