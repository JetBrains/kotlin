/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.ide.impl.convert;

import com.intellij.util.xmlb.annotations.XCollection;

import java.util.ArrayList;
import java.util.List;

/**
* @author nik
*/
public class ProjectFileVersionState {
  private List<String> myPerformedConversionIds = new ArrayList<>();

  @XCollection(propertyElementName = "performed-conversions", elementName = "converter", valueAttributeName = "id")
  public List<String> getPerformedConversionIds() {
    return myPerformedConversionIds;
  }

  public void setPerformedConversionIds(List<String> performedConversionIds) {
    myPerformedConversionIds = performedConversionIds;
  }
}
