// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.personalization.impl

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

/**
 * @author Vitaliy.Bibaev
 */
@State(name = "ProjectUserFactors", storages = [
  Storage(StoragePathMacros.CACHE_FILE),
  Storage(StoragePathMacros.WORKSPACE_FILE, deprecated = true)
],
reportStatistic = false)
class ProjectUserFactorStorage : UserFactorStorageBase()