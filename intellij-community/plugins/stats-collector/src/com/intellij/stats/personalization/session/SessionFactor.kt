// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

interface SessionFactor {
  val simpleName: String

  interface LookupBased : SessionFactor {
    fun getValue(storage: LookupFactorsStorage): Any?
  }

  interface LookupElementBased : SessionFactor {
    fun getValue(storage: LookupElementFactorsStorage): Any?
  }
}
