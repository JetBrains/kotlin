// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

internal interface InsetValueProvider {
  val left: Int
    get() = 0
  val right: Int
    get() = 0
  val top: Int
    get() = 0
  val down: Int
    get() = 0
}