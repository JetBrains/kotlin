// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.parameter

import com.intellij.codeInsight.hints.filtering.MatcherConstructor

class ParameterBlackListFilter(blackList: Set<String>) {
  val matchers = blackList.mapNotNull { MatcherConstructor.createMatcher(it) }

  fun shouldShowHint(info: BlackListInfo): Boolean {
    val methodName = info.fullyQualifiedName
    val parameterNames = info.parameterNames
    return matchers.none { it.isMatching(methodName, parameterNames) }
  }
}
