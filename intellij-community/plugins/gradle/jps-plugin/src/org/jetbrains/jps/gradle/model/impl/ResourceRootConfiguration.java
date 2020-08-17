/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.util.xmlb.annotations.Attribute;
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
@Tag("resource")
public class ResourceRootConfiguration extends FilePattern {
  @Tag("directory")
  @NotNull
  public String directory;

  @Tag("targetPath")
  @Nullable
  public String targetPath;

  @Attribute("filtered")
  public boolean isFiltered;

  @XCollection(propertyElementName = "filters", elementName = "filter")
  public List<ResourceRootFilter> filters = new ArrayList<>();

  public int computeConfigurationHash(@Nullable PathRelativizerService pathRelativizerService) {
    int result;
    if(pathRelativizerService == null) {
      result = directory.hashCode();
      result = 31 * result + (targetPath != null ? targetPath.hashCode() : 0);
    } else {
      result = pathRelativizerService.toRelative(directory).hashCode();
      result = 31 * result + (targetPath != null ? pathRelativizerService.toRelative(targetPath).hashCode() : 0);
    }

    result = 31 * result + (isFiltered ? 1 : 0);
    result = 31 * result + includes.hashCode();
    result = 31 * result + excludes.hashCode();
    for (ResourceRootFilter filter : filters) {
      result = 31 * result + filter.computeConfigurationHash();
    }
    return result;
  }
}
