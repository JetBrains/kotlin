// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exportable part of file template settings. User-specific (local) settings are handled by FileTemplateManagerImpl.
 *
 * @author Rustam Vishnyakov
 */
@State(
  name = "ExportableFileTemplateSettings",
  storages = @Storage(FileTemplateSettings.EXPORTABLE_SETTINGS_FILE)
)
class FileTemplateSettings extends FileTemplatesLoader implements PersistentStateComponent<Element> {
  static final String EXPORTABLE_SETTINGS_FILE = "file.template.settings.xml";

  private static final String ELEMENT_TEMPLATE = "template";
  private static final String ATTRIBUTE_NAME = "name";
  private static final String ATTRIBUTE_REFORMAT = "reformat";
  private static final String ATTRIBUTE_LIVE_TEMPLATE = "live-template-enabled";
  private static final String ATTRIBUTE_ENABLED = "enabled";

  FileTemplateSettings(@Nullable Project project) {
    super(project);
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("fileTemplateSettings");

    for (FTManager manager : getAllManagers()) {
      Element templatesGroup = null;
      for (FileTemplateBase template : manager.getAllTemplates(true)) {
        // save only those settings that differ from defaults
        boolean shouldSave = template.isReformatCode() != FileTemplateBase.DEFAULT_REFORMAT_CODE_VALUE ||
                             // check isLiveTemplateEnabledChanged() first to avoid expensive loading all templates on exit
                             template.isLiveTemplateEnabledChanged() && template.isLiveTemplateEnabled() != template.isLiveTemplateEnabledByDefault();
        if (template instanceof BundledFileTemplate) {
          shouldSave |= ((BundledFileTemplate)template).isEnabled() != FileTemplateBase.DEFAULT_ENABLED_VALUE;
        }
        if (!shouldSave) continue;

        final Element templateElement = new Element(ELEMENT_TEMPLATE);
        templateElement.setAttribute(ATTRIBUTE_NAME, template.getQualifiedName());
        templateElement.setAttribute(ATTRIBUTE_REFORMAT, Boolean.toString(template.isReformatCode()));
        templateElement.setAttribute(ATTRIBUTE_LIVE_TEMPLATE, Boolean.toString(template.isLiveTemplateEnabled()));

        if (template instanceof BundledFileTemplate) {
          templateElement.setAttribute(ATTRIBUTE_ENABLED, Boolean.toString(((BundledFileTemplate)template).isEnabled()));
        }

        if (templatesGroup == null) {
          templatesGroup = new Element(getXmlElementGroupName(manager));
          element.addContent(templatesGroup);
        }
        templatesGroup.addContent(templateElement);
      }
    }

    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    for (final FTManager manager : getAllManagers()) {
      final Element templatesGroup = state.getChild(getXmlElementGroupName(manager));
      if (templatesGroup == null) continue;

      for (Element child : templatesGroup.getChildren(ELEMENT_TEMPLATE)) {
        final String qName = child.getAttributeValue(ATTRIBUTE_NAME);
        final FileTemplateBase template = manager.getTemplate(qName);
        if (template == null) continue;

        template.setReformatCode(Boolean.parseBoolean(child.getAttributeValue(ATTRIBUTE_REFORMAT)));
        template.setLiveTemplateEnabled(Boolean.parseBoolean(child.getAttributeValue(ATTRIBUTE_LIVE_TEMPLATE)));

        if (template instanceof BundledFileTemplate) {
          ((BundledFileTemplate)template).setEnabled(Boolean.parseBoolean(child.getAttributeValue(ATTRIBUTE_ENABLED, "true")));
        }
      }
    }
  }

  private static String getXmlElementGroupName(@NotNull FTManager manager) {
    return StringUtil.toLowerCase(manager.getName()) + "_templates";
  }
}