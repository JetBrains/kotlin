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

package com.intellij.openapi.paths;

import com.intellij.psi.PsiElement;
import com.intellij.psi.templateLanguages.OuterLanguageElement;

/**
 * @author Dmitry Avdeev
 */
public class GenericDynamicContextProvider implements DynamicContextProvider {

  @Override
  public int getOffset(PsiElement element, int offset, String elementText) {
    final PsiElement[] children = element.getChildren();
    for (PsiElement child : children) {
      if (isDynamic(child)) {
        final int i = child.getStartOffsetInParent();
        if (i == offset) {  // dynamic context?
          final PsiElement next = child.getNextSibling();
          if (next == null || !next.getText().startsWith("/")) {
            return -1;
          }
          offset = next.getStartOffsetInParent();
        } else {
          final int pos = PathReferenceProviderBase.getLastPosOfURL(offset, elementText);
          if (pos == -1 || pos > i) {
            return -1;
          }
          return offset;
        }
      }
    }
    return offset;
  }

  protected boolean isDynamic(PsiElement child) {
    return child instanceof OuterLanguageElement;
  }

}
