// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion

import com.intellij.internal.statistic.DeviceIdManager

interface InstallationIdProvider {
    fun installationId(): String
}

class PermanentInstallationIdProvider: InstallationIdProvider {
    override fun installationId(): String = DeviceIdManager.getOrGenerateId()
}
