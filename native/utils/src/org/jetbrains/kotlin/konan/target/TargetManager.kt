/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

interface TargetManager {
    val target: KonanTarget
    val targetName: String
    fun list()
}

internal class TargetManagerImpl(val userRequest: String?, val hostManager: HostManager) : TargetManager {
    override val target = determineCurrent()
    override val targetName
        get() = target.visibleName

    override fun list() {
        hostManager.enabled.forEach {
            val isDefault = if (it == target) " (default)" else ""
            val isDeprecated = if (it in KonanTarget.deprecatedTargets) " (deprecated)" else ""
            println("${it.visibleName}$isDefault$isDeprecated")
        }
    }

    private fun determineCurrent(): KonanTarget {
        return if (userRequest == null || userRequest == "host") {
            HostManager.host
        } else {
            val resolvedAlias = HostManager.resolveAlias(userRequest)
            hostManager.targets.getValue(hostManager.known(resolvedAlias))
        }
    }
}

fun hostTargetSuffix(host: KonanTarget, target: KonanTarget) =
    if (target == host) host.name else "${host.name}-${target.name}"
