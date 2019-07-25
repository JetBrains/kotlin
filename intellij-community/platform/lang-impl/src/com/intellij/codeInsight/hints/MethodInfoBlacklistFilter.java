/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.hints;


import com.intellij.codeInsight.hints.filtering.Matcher;
import com.intellij.codeInsight.hints.filtering.MatcherConstructor;
import com.intellij.codeInsight.hints.settings.Diff;
import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings;
import com.intellij.lang.Language;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.codeInsight.hints.HintUtilsKt.getLanguageForSettingKey;

public class MethodInfoBlacklistFilter implements HintInfoFilter {
  private final List<Matcher> myMatchers;

  public MethodInfoBlacklistFilter(Set<String> list) {
    myMatchers = list.stream()
      .map((item) -> MatcherConstructor.INSTANCE.createMatcher(item))
      .filter((e) -> e != null)
      .collect(Collectors.toList());
  }

  @NotNull
  public static MethodInfoBlacklistFilter forLanguage(@NotNull Language language) {
    Set<String> list = fullBlacklist(language);
    return new MethodInfoBlacklistFilter(list);
  }

  @Override
  public boolean showHint(@NotNull HintInfo info) {
    if (info instanceof HintInfo.MethodInfo) {
      HintInfo.MethodInfo methodInfo = (HintInfo.MethodInfo)info;
      return myMatchers.stream()
        .noneMatch((e) -> e.isMatching(methodInfo.getFullyQualifiedName(), methodInfo.getParamNames()));
    }
    return false;
  }

  @NotNull
  private static Set<String> fullBlacklist(Language language) {
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (provider == null) {
      return ContainerUtil.newHashOrEmptySet(ContainerUtil.emptyIterable());
    }

    Set<String> blackList = blacklist(language);
    Language dependentLanguage = provider.getBlackListDependencyLanguage();
    if (dependentLanguage != null) {
      blackList.addAll(blacklist(dependentLanguage));
    }
    return blackList;
  }

  @NotNull
  private static Set<String> blacklist(@NotNull Language language) {
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (provider != null) {
      ParameterNameHintsSettings settings = ParameterNameHintsSettings.getInstance();
      Diff diff = settings.getBlackListDiff(getLanguageForSettingKey(language));
      return diff.applyOn(provider.getDefaultBlackList());
    }
    return ContainerUtil.newHashOrEmptySet(ContainerUtil.emptyIterable());
  }

}