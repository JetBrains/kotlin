// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.impl

import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "ApplicationUserFactors", storages = [(Storage(value = "completion.factors.user.xml", roamingType = RoamingType.DISABLED))], reportStatistic = false)
class ApplicationUserFactorStorage : UserFactorStorageBase()