// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

/**
 * Matching prefixes for *isEmptyString* lookup element as example:
 *  - `isempt` -> [START]
 *  - `isEmpt` -> [START]
 *  - `ies` -> [FIRST_CHARS]
 *  - `iES` -> [FIRST_CHARS]
 *  - `isEmpSt` -> [SYMBOLS_WITH_CASE]
 *  - `EmpSt` -> [SYMBOLS_WITH_CASE]
 *  - `isempst` -> [SYMBOLS]
 *  - `Emstr` -> [SYMBOLS]
 */
enum class PrefixMatchingType {
  START,
  FIRST_CHARS,
  SYMBOLS_WITH_CASE,
  SYMBOLS,
  UNKNOWN
}