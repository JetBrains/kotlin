// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform.templates;

import com.intellij.facet.frameworks.beans.Artifact;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ProjectTemplateParameterFactory;
import com.intellij.ide.util.projectWizard.WizardInputField;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.platform.ProjectTemplate;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * @author Dmitry Avdeev
 */
@Tag("template")
public abstract class ArchivedProjectTemplate implements ProjectTemplate {
  public static final String INPUT_FIELD = "input-field";
  public static final String TEMPLATE = "template";
  public static final String INPUT_DEFAULT = "default";

  protected final String myDisplayName;
  @Nullable private final String myCategory;

  private List<WizardInputField> myInputFields = Collections.emptyList();
  private List<String> myFrameworks = new ArrayList<>();
  private List<Artifact> myArtifacts = new ArrayList<>();

  public ArchivedProjectTemplate(@NotNull String displayName, @Nullable String category) {
    myDisplayName = displayName;
    myCategory = category;
  }

  @NotNull
  @Override
  public String getName() {
    return myDisplayName;
  }

  @Override
  public Icon getIcon() {
    return getModuleType().getIcon();
  }

  protected abstract ModuleType getModuleType();

  @NotNull
  @Override
  public ModuleBuilder createModuleBuilder() {
    return new TemplateModuleBuilder(this, getModuleType(), getInputFields());
  }

  @NotNull
  public List<WizardInputField> getInputFields() {
    return myInputFields;
  }

  @Property(surroundWithTag = false)
  @XCollection(elementName = "artifact")
  public List<Artifact> getArtifacts() {
    return myArtifacts;
  }

  public void setArtifacts(List<Artifact> artifacts) {
    myArtifacts = artifacts;
  }

  @NotNull
  @Property(surroundWithTag = false)
  @XCollection(elementName = "framework", valueAttributeName = "")
  public List<String> getFrameworks() {
    return myFrameworks;
  }

  public void setFrameworks(List<String> frameworks) {
    myFrameworks = frameworks;
  }

  @Nullable
  @Override
  public ValidationInfo validateSettings() {
    return null;
  }

  public void handleUnzippedDirectories(@NotNull File dir, @NotNull List<? super File> filesToRefresh) throws IOException {
    filesToRefresh.add(dir);
  }

  public static abstract class StreamProcessor<T> {
    public abstract T consume(@NotNull ZipInputStream stream) throws IOException;
  }

  public abstract <T> T processStream(@NotNull StreamProcessor<T> consumer) throws IOException;

  @Nullable
  public String getCategory() {
    return myCategory;
  }

  public void populateFromElement(@NotNull Element element) {
    XmlSerializer.deserializeInto(this, element);
    myInputFields = getFields(element);
  }

  private static List<WizardInputField> getFields(Element templateElement) {
    return ContainerUtil
      .mapNotNull(templateElement.getChildren(INPUT_FIELD), element -> {
        ProjectTemplateParameterFactory factory = WizardInputField.getFactoryById(element.getText());
        return factory == null ? null : factory.createField(element.getAttributeValue(INPUT_DEFAULT));
      });
  }

  protected static <T> T consumeZipStream(@NotNull StreamProcessor<T> consumer, @NotNull ZipInputStream stream) throws IOException {
    try {
      return consumer.consume(stream);
    }
    finally {
      StreamUtil.closeStream(stream);
    }
  }
}
