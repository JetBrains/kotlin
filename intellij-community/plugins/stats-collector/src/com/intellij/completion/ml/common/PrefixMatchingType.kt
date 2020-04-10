// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

/**
 * Matching prefixes for *isEmptyString* lookup element as example:
 *  - `isempt` -> [START_WITH]
 *  - `isEmpt` -> [START_WITH]
 *  - `ies` -> [WORDS_FIRST_CHAR]
 *  - `iES` -> [WORDS_FIRST_CHAR]
 *  - `isEmpSt` -> [GREEDY_WITH_CASE]
 *  - `EmpSt` -> [GREEDY_WITH_CASE]
 *  - `isempst` -> [GREEDY]
 *  - `Emstr` -> [GREEDY]
 */
enum class PrefixMatchingType {
  START_WITH,
  WORDS_FIRST_CHAR,
  GREEDY_WITH_CASE,
  GREEDY,
  UNKNOWN
}