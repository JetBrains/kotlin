// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;

public final class DefaultExternalFilter implements ExternalFilter {
  private static final long serialVersionUID = 1L;

  @NotNull
  private String filterType;
  @NotNull
  private String propertiesAsJsonMap;

  public DefaultExternalFilter() {
    propertiesAsJsonMap = "";
    filterType = "";
  }


  public DefaultExternalFilter(ExternalFilter filter) {
    propertiesAsJsonMap = filter.getPropertiesAsJsonMap();
    filterType = filter.getFilterType();
  }

  @NotNull
  @Override
  public String getFilterType() {
    return filterType;
  }

  public void setFilterType(@NotNull String filterType) {
    this.filterType = filterType;
  }

  @Override
  @NotNull
  public String getPropertiesAsJsonMap() {
    return propertiesAsJsonMap;
  }

  public void setPropertiesAsJsonMap(@NotNull String propertiesAsJsonMap) {
    this.propertiesAsJsonMap = propertiesAsJsonMap;
  }
}
