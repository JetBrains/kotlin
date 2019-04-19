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
package com.intellij.ide.actions;

import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.LineMarkersPass;
import com.intellij.lang.Language;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author nik
 */
public class RelatedItemLineMarkerGotoAdapter extends GotoRelatedProvider {
  @NotNull
  @Override
  public List<? extends GotoRelatedItem> getItems(@NotNull PsiElement context) {
    List<PsiElement> parents = new ArrayList<>();
    PsiElement current = context;
    Set<Language> languages = new HashSet<>();
    while (current != null) {
      parents.add(current);
      languages.add(current.getLanguage());
      if (current instanceof PsiFile) break;
      current = current.getParent();
    }

    List<LineMarkerProvider> providers = new ArrayList<>();
    for (Language language : languages) {
      providers.addAll(LineMarkersPass.getMarkerProviders(language, context.getProject()));
    }
    List<GotoRelatedItem> items = new ArrayList<>();
    for (LineMarkerProvider provider : providers) {
      if (provider instanceof RelatedItemLineMarkerProvider) {
        List<RelatedItemLineMarkerInfo> markers = new ArrayList<>();
        RelatedItemLineMarkerProvider relatedItemLineMarkerProvider = (RelatedItemLineMarkerProvider)provider;
        for (PsiElement parent : parents) {
          ContainerUtil.addIfNotNull(markers, relatedItemLineMarkerProvider.getLineMarkerInfo(parent));
        }
        relatedItemLineMarkerProvider.collectNavigationMarkers(parents, markers, true);

        addItemsForMarkers(markers, items);
      }
    }

    return items;
  }

  private static void addItemsForMarkers(List<? extends RelatedItemLineMarkerInfo> markers,
                                         List<? super GotoRelatedItem> result) {
    Set<PsiFile> addedFiles = new HashSet<>();
    for (RelatedItemLineMarkerInfo<?> marker : markers) {
      Collection<? extends GotoRelatedItem> items = marker.createGotoRelatedItems();
      for (GotoRelatedItem item : items) {
        PsiElement element = item.getElement();
        if (element instanceof PsiFile) {
          PsiFile file = (PsiFile)element;
          if (addedFiles.contains(file)) {
            continue;
          }
        }
        if (element != null) {
          ContainerUtil.addIfNotNull(addedFiles, element.getContainingFile());
        }
        result.add(item);
      }
    }
  }
}
