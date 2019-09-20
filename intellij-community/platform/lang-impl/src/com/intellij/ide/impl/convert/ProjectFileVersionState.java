// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.impl.convert;

import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
* @author nik
*/
public final class ProjectFileVersionState {
  @XCollection(propertyElementName = "performed-conversions", elementName = "converter", valueAttributeName = "id")
  private final List<String> myPerformedConversionIds;

  public ProjectFileVersionState() {
    myPerformedConversionIds = new ArrayList<>();
  }

  public ProjectFileVersionState(@NotNull List<String> performedConversionIds) {
    myPerformedConversionIds = performedConversionIds;
  }

  public List<String> getPerformedConversionIds() {
    return myPerformedConversionIds;
  }
}
