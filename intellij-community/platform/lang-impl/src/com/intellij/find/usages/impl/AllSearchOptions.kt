// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages.impl

import com.intellij.find.usages.UsageOptions
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class AllSearchOptions<O>(
  val options: UsageOptions,
  val textSearch: Boolean?,
  val customOptions: O
)
