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

package com.intellij.usageView;

import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.meta.PsiPresentableMetaData;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class UsageViewNodeTextLocation extends ElementDescriptionLocation {
  private UsageViewNodeTextLocation() { }

  public static final UsageViewNodeTextLocation INSTANCE = new UsageViewNodeTextLocation();

  @NotNull
  @Override
  public ElementDescriptionProvider getDefaultProvider() {
    return DEFAULT_PROVIDER;
  }

  private static final ElementDescriptionProvider DEFAULT_PROVIDER = (element, location) -> {
    if (!(location instanceof UsageViewNodeTextLocation)) return null;

    if (element instanceof PsiMetaOwner) {
      final PsiMetaData metaData = ((PsiMetaOwner)element).getMetaData();
      if (metaData instanceof PsiPresentableMetaData) {
        return ((PsiPresentableMetaData)metaData).getTypeName() + " " + DescriptiveNameUtil.getMetaDataName(metaData);
      }
    }

    if (element instanceof PsiFile) {
      return ((PsiFile)element).getName();
    }

    return LanguageFindUsages.getNodeText(element, true);
  };
}