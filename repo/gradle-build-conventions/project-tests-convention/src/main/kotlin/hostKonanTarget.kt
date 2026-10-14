/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.attributes.AttributeContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

/**
 * Requests the Kotlin/Native variant for the current host.
 * Does nothing on hosts unsupported by Kotlin/Native, e.g. linux-aarch64 (KT-86717).
 *
 * WARNING: the result depends on the host, so the same build may resolve differently on another OS.
 */
fun AttributeContainer.hostKonanTargetAttribute() {
    val host = HostManager.hostOrNull ?: return
    attribute(KotlinNativeTarget.konanTargetAttribute, host.name)
}
