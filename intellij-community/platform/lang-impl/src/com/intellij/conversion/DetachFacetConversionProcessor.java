/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.conversion;

import com.intellij.facet.FacetManagerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class DetachFacetConversionProcessor extends ConversionProcessor<ModuleSettings> {
  private final String[] myFacetNames;

  public DetachFacetConversionProcessor(@NotNull String... names) {
    myFacetNames = names;
  }

  @Override
  public boolean isConversionNeeded(ModuleSettings moduleSettings) {
    for (String facetName : myFacetNames) {
      if (facetName != null && !moduleSettings.getFacetElements(facetName).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void process(ModuleSettings moduleSettings) throws CannotConvertException {
    final Element facetManagerElement = moduleSettings.getComponentElement(FacetManagerImpl.COMPONENT_NAME);
    if (facetManagerElement == null) return;
    for (String facetName : myFacetNames) {
      for (Element element : getElements(moduleSettings, facetName)) {
        element.detach();
      }
    }
  }

  private static Element[] getElements(ModuleSettings moduleSettings, String facetName) {
    Collection<? extends Element> elements = moduleSettings.getFacetElements(facetName);
    return elements.toArray(new Element[0]);
  }
}
