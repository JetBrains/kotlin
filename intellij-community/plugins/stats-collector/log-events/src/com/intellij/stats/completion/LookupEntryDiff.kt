// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

data class LookupEntryDiff(val id: Int, val added: Map<String, String?>, val changed: Map<String, String?>, val removed: List<String>)
