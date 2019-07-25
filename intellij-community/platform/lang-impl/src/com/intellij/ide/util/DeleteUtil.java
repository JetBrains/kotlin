/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.intellij.ide.util;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.FactoryMap;
import java.util.HashMap;

import java.text.MessageFormat;
import java.util.Map;

/**
 * @author dsl
 */
public class DeleteUtil {
  private DeleteUtil() { }

  public static String generateWarningMessage(String messageTemplate, final PsiElement[] elements) {
    if (elements.length == 1) {
      String name = ElementDescriptionUtil.getElementDescription(elements[0], DeleteNameDescriptionLocation.INSTANCE);
      String type = ElementDescriptionUtil.getElementDescription(elements[0], DeleteTypeDescriptionLocation.SINGULAR);
      return MessageFormat.format(messageTemplate, type + (StringUtil.isEmptyOrSpaces(name) ? "" : " \"" + name + "\""));
    }

    Map<String, Integer> countMap = FactoryMap.create(key -> 0);
    Map<String, String> pluralToSingular = new HashMap<>();
    int directoryCount = 0;
    String containerType = null;

    for (final PsiElement elementToDelete : elements) {
      String type = ElementDescriptionUtil.getElementDescription(elementToDelete, DeleteTypeDescriptionLocation.PLURAL);
      pluralToSingular.put(type, ElementDescriptionUtil.getElementDescription(elementToDelete, DeleteTypeDescriptionLocation.SINGULAR));
      int oldCount = countMap.get(type).intValue();
      countMap.put(type, oldCount+1);
      if (elementToDelete instanceof PsiDirectoryContainer) {
        containerType = type;
        directoryCount += ((PsiDirectoryContainer) elementToDelete).getDirectories().length;
      }
    }

    StringBuilder buffer = new StringBuilder();
    for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
      if (buffer.length() > 0) {
        if (buffer.length() > 0) {
          buffer.append(" ").append(IdeBundle.message("prompt.delete.and")).append(" ");
        }
      }
      final int count = entry.getValue().intValue();

      buffer.append(count).append(" ");
      if (count == 1) {
        buffer.append(pluralToSingular.get(entry.getKey()));
      }
      else {
        buffer.append(entry.getKey());
      }

      if (entry.getKey().equals(containerType)) {
        buffer.append(" ").append(IdeBundle.message("prompt.delete.directory.paren", directoryCount));
      }
    }
    return MessageFormat.format(messageTemplate, buffer.toString());
  }
}
