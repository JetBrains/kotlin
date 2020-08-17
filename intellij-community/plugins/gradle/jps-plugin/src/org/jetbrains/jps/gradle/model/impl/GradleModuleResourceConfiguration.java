/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.incremental.relativizer.PathRelativizerService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleModuleResourceConfiguration {
  @NotNull
  @Tag("id")
  public ModuleVersion id;

  @Nullable
  @Tag("parentId")
  public ModuleVersion parentId;

  @OptionTag
  public boolean overwrite;

  @OptionTag
  public String outputDirectory = null;

  @XCollection(propertyElementName = "resources", elementName = "resource")
  public List<ResourceRootConfiguration> resources = new ArrayList<>();

  @XCollection(propertyElementName = "test-resources", elementName = "resource")
  public List<ResourceRootConfiguration> testResources = new ArrayList<>();

  public int computeConfigurationHash(boolean forTestResources, PathRelativizerService pathRelativizerService) {
    int result = computeModuleConfigurationHash();

    final List<ResourceRootConfiguration> _resources = forTestResources ? testResources : resources;
    result = 31 * result;
    for (ResourceRootConfiguration resource : _resources) {
      result += resource.computeConfigurationHash(pathRelativizerService);
    }
    return result;
  }

  public int computeConfigurationHash() {
    int result = computeModuleConfigurationHash();

    final List<ResourceRootConfiguration> _resources = ContainerUtil.concat(testResources, resources);
    result = 31 * result;
    for (ResourceRootConfiguration resource : _resources) {
      result += resource.computeConfigurationHash(null);
    }
    return result;
  }

  public int computeModuleConfigurationHash() {
    int result = id.hashCode();
    result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
    result = 31 * result + (outputDirectory != null ? outputDirectory.hashCode() : 0);
    result = 31 * result + (overwrite ? 1 : 0);
    return result;
  }
}



