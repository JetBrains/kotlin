// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.artifacts.ArchiveAntCopyInstructionCreator;
import com.intellij.compiler.ant.taskdefs.Jar;
import com.intellij.compiler.ant.taskdefs.Zip;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.ui.ArchiveElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ArchivePackagingElement extends CompositeElementWithManifest<ArchivePackagingElement> {
  @NonNls public static final String NAME_ATTRIBUTE = "name";
  private String myArchiveFileName;

  public ArchivePackagingElement() {
    super(PackagingElementFactoryImpl.ARCHIVE_ELEMENT_TYPE);
  }

  public ArchivePackagingElement(@NotNull String archiveFileName) {
    super(PackagingElementFactoryImpl.ARCHIVE_ELEMENT_TYPE);
    myArchiveFileName = archiveFileName;
  }

  @Override
  @NotNull
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new ArchiveElementPresentation(this);
  }

  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator,
                                                          @NotNull ArtifactAntGenerationContext generationContext,
                                                          @NotNull ArtifactType artifactType) {
    final String tempJarProperty = generationContext.createNewTempFileProperty("temp.jar.path." + myArchiveFileName, myArchiveFileName);
    String jarPath = BuildProperties.propertyRef(tempJarProperty);
    final Tag jar;
    if (myArchiveFileName.endsWith(".jar")) {
      jar = new Jar(jarPath, "preserve", true);
    }
    else {
      jar = new Zip(jarPath);
    }
    for (Generator generator : computeChildrenGenerators(resolvingContext, new ArchiveAntCopyInstructionCreator(""), generationContext, artifactType)) {
      jar.add(generator);
    }
    generationContext.runBeforeCurrentArtifact(jar);
    return Collections.singletonList(creator.createFileCopyInstruction(jarPath, myArchiveFileName));
  }

  @Attribute(NAME_ATTRIBUTE)
  public String getArchiveFileName() {
    return myArchiveFileName;
  }

  @NonNls @Override
  public String toString() {
    return "archive:" + myArchiveFileName;
  }

  @Override
  public ArchivePackagingElement getState() {
    return this;
  }

  public void setArchiveFileName(String archiveFileName) {
    myArchiveFileName = archiveFileName;
  }

  @Override
  public String getName() {
    return myArchiveFileName;
  }

  @Override
  public void rename(@NotNull String newName) {
    myArchiveFileName = newName;
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element instanceof ArchivePackagingElement && ((ArchivePackagingElement)element).getArchiveFileName().equals(myArchiveFileName);
  }

  @Override
  public void loadState(@NotNull ArchivePackagingElement state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
