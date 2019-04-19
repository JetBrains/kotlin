/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ComponentManagerSettings;
import com.intellij.conversion.ModuleSettings;
import com.intellij.facet.FacetManagerImpl;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.impl.*;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.JDomSerializationUtil;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class ModuleSettingsImpl extends ComponentManagerSettingsImpl implements ModuleSettings {
  private final String myModuleName;

  public ModuleSettingsImpl(File moduleFile, ConversionContextImpl context) throws CannotConvertException {
    super(moduleFile, context);
    myModuleName = getModuleName(moduleFile);
  }

  public static String getModuleName(File moduleFile) {
    return StringUtil.trimEnd(moduleFile.getName(), ModuleFileType.DOT_DEFAULT_EXTENSION);
  }

  @Override
  @NotNull
  public String getModuleName() {
    return myModuleName;
  }

  @Override
  @Nullable
  public String getModuleType() {
    return getRootElement().getAttributeValue(Module.ELEMENT_TYPE);
  }

  @Override
  @NotNull
  public File getModuleFile() {
    return mySettingsFile.getFile();
  }

  @Override
  @NotNull
  public Collection<? extends Element> getFacetElements(@NotNull String facetTypeId) {
    final Element facetManager = getComponentElement(FacetManagerImpl.COMPONENT_NAME);
    final ArrayList<Element> elements = new ArrayList<>();

    addFacetTypes(facetTypeId, facetManager, elements);

    return elements;
  }

  private static void addFacetTypes(@NotNull String facetTypeId, @Nullable Element parent, @NotNull ArrayList<? super Element> elements) {
    for (Element child : JDOMUtil.getChildren(parent, JpsFacetSerializer.FACET_TAG)) {
      if (facetTypeId.equals(child.getAttributeValue(JpsFacetSerializer.TYPE_ATTRIBUTE))) {
        elements.add(child);
      } else {
        addFacetTypes(facetTypeId, child, elements);
      }
    }
  }

  @Override
  public Element getFacetElement(@NotNull String facetTypeId) {
    return ContainerUtil.getFirstItem(getFacetElements(facetTypeId), null);
  }

  @Override
  public void addFacetElement(@NotNull String facetTypeId, @NotNull String facetName, Element configuration) {
    Element componentElement = JDomSerializationUtil.findOrCreateComponentElement(getRootElement(), FacetManagerImpl.COMPONENT_NAME);
    Element facetElement = new Element(JpsFacetSerializer.FACET_TAG);
    facetElement.setAttribute(JpsFacetSerializer.TYPE_ATTRIBUTE, facetTypeId);
    facetElement.setAttribute(JpsFacetSerializer.NAME_ATTRIBUTE, facetName);
    configuration.setName(JpsFacetSerializer.CONFIGURATION_TAG);
    facetElement.addContent(configuration);
    componentElement.addContent(facetElement);
  }

  @Override
  public void setModuleType(@NotNull String moduleType) {
    getRootElement().setAttribute(Module.ELEMENT_TYPE, moduleType);
  }

  @Override
  @NotNull
  public String expandPath(@NotNull String path) {
    return myContext.expandPath(path, this);
  }

  @NotNull
  @Override
  public String collapsePath(@NotNull String path) {
    return ConversionContextImpl.collapsePath(path, this);
  }

  @Override
  @NotNull
  public Collection<File> getSourceRoots(boolean includeTests) {
    final List<File> result = new ArrayList<>();
    for (Element contentRoot : getContentRootElements()) {
      for (Element sourceFolder : JDOMUtil.getChildren(contentRoot, SourceFolderImpl.ELEMENT_NAME)) {
        boolean isTestFolder = Boolean.parseBoolean(sourceFolder.getAttributeValue(SourceFolderImpl.TEST_SOURCE_ATTR));
        if (includeTests || !isTestFolder) {
          result.add(getFile(sourceFolder.getAttributeValue(SourceFolderImpl.URL_ATTRIBUTE)));
        }
      }
    }
    return result;
  }

  private List<Element> getContentRootElements() {
    return JDOMUtil.getChildren(getComponentElement(MODULE_ROOT_MANAGER_COMPONENT), ContentEntryImpl.ELEMENT_NAME);
  }

  @Override
  @NotNull
  public Collection<File> getContentRoots() {
    final List<File> result = new ArrayList<>();
    for (Element contentRoot : getContentRootElements()) {
      String path = VfsUtil.urlToPath(contentRoot.getAttributeValue(ContentEntryImpl.URL_ATTRIBUTE));
      result.add(new File(FileUtil.toSystemDependentName(expandPath(path))));
    }
    return result;
  }

  @Override
  @Nullable
  public String getProjectOutputUrl() {
    final ComponentManagerSettings rootManagerSettings = myContext.getProjectRootManagerSettings();
    final Element projectRootManager = rootManagerSettings == null ? null : rootManagerSettings.getComponentElement("ProjectRootManager");
    final Element outputElement = projectRootManager == null ? null : projectRootManager.getChild("output");
    return outputElement == null ? null : outputElement.getAttributeValue("url");
  }

  @Override
  public void addExcludedFolder(@NotNull File directory) {
    final ComponentManagerSettings rootManagerSettings = myContext.getProjectRootManagerSettings();
    if (rootManagerSettings != null) {
      final Element projectRootManager = rootManagerSettings.getComponentElement("ProjectRootManager");
      if (projectRootManager != null) {
        final Element outputElement = projectRootManager.getChild("output");
        if (outputElement != null) {
          final String outputUrl = outputElement.getAttributeValue("url");
          if (outputUrl != null) {
            final File outputFile = getFile(outputUrl);
            if (FileUtil.isAncestor(outputFile, directory, false)) {
              return;
            }
          }
        }
      }
    }
    for (Element contentRoot : getContentRootElements()) {
      final File root = getFile(contentRoot.getAttributeValue(ContentEntryImpl.URL_ATTRIBUTE));
      if (FileUtil.isAncestor(root, directory, true)) {
        addExcludedFolder(directory, contentRoot);
      }
    }
  }

  @Override
  @NotNull
  public List<File> getModuleLibraryRoots(String libraryName) {
    final Element library = findModuleLibraryElement(libraryName);
    return library != null ? myContext.getClassRoots(library, this) : Collections.emptyList();
  }

  @Override
  public boolean hasModuleLibrary(String libraryName) {
    return findModuleLibraryElement(libraryName) != null;
  }

  @Nullable
  private Element findModuleLibraryElement(String libraryName) {
    for (Element element : getOrderEntries()) {
      if (ModuleLibraryOrderEntryImpl.ENTRY_TYPE.equals(element.getAttributeValue(OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR))) {
        final Element library = element.getChild(LibraryImpl.ELEMENT);
        if (library != null && libraryName.equals(library.getAttributeValue(LibraryImpl.LIBRARY_NAME_ATTR))) {
          return library;
        }
      }
    }
    return null;
  }

  @Override
  public List<Element> getOrderEntries() {
    final Element component = getComponentElement(MODULE_ROOT_MANAGER_COMPONENT);
    return JDOMUtil.getChildren(component, OrderEntryFactory.ORDER_ENTRY_ELEMENT_NAME);
  }

  @Override
  @NotNull
  public Collection<ModuleSettings> getAllModuleDependencies() {
    Set<ModuleSettings> dependencies = new HashSet<>();
    collectDependencies(dependencies);
    return dependencies;
  }

  private void collectDependencies(Set<ModuleSettings> dependencies) {
    if (!dependencies.add(this)) {
      return;
    }

    for (Element element : getOrderEntries()) {
      if (ModuleOrderEntryImpl.ENTRY_TYPE.equals(element.getAttributeValue(OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR))) {
        final String moduleName = element.getAttributeValue(ModuleOrderEntryImpl.MODULE_NAME_ATTR);
        if (moduleName != null) {
          final ModuleSettings moduleSettings = myContext.getModuleSettings(moduleName);
          if (moduleSettings != null) {
            ((ModuleSettingsImpl)moduleSettings).collectDependencies(dependencies);
          }
        }
      }
    }
  }

  private void addExcludedFolder(File directory, Element contentRoot) {
    for (Element excludedFolder : JDOMUtil.getChildren(contentRoot, ExcludeFolderImpl.ELEMENT_NAME)) {
      final File excludedDir = getFile(excludedFolder.getAttributeValue(ExcludeFolderImpl.URL_ATTRIBUTE));
      if (FileUtil.isAncestor(excludedDir, directory, false)) {
        return;
      }
    }
    String path = ConversionContextImpl.collapsePath(FileUtil.toSystemIndependentName(directory.getAbsolutePath()), this);
    contentRoot.addContent(new Element(ExcludeFolderImpl.ELEMENT_NAME).setAttribute(ExcludeFolderImpl.URL_ATTRIBUTE, VfsUtil.pathToUrl(path)));
  }

  private File getFile(String url) {
    return new File(FileUtil.toSystemDependentName(expandPath(VfsUtil.urlToPath(url))));
  }
}
