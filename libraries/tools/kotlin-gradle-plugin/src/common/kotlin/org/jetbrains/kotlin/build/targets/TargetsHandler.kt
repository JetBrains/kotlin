/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.targets

import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.konan.target.KonanTarget

typealias TargetId = String

class KotlinTarget(
    val id: TargetId,
    val type: TargetType
)

sealed class TargetType {
    object JVM : TargetType()
    class Native(val konanTarget: KonanTarget): TargetType()
    object JS: TargetType()
    class WASM(val wasmType: KotlinWasmTargetType): TargetType()

    object AndroidJvm : TargetType()
}

data class CreateTarget(
    val id: TargetId,
    val type: TargetType
)

class TargetsHandler(
    private val eventsListener: TargetEventsListener
) {
    private val targets: MutableMap<TargetId, KotlinTarget> = mutableMapOf()

    sealed class Error : RuntimeException() {
        class TargetAlreadyExists(val id: TargetId) : Error()
    }

    fun createTarget(targetId: TargetId, targetType: TargetType, context: Any?) {
        if (targetId in targets) throw Error.TargetAlreadyExists(targetId)
        val target = KotlinTarget(targetId, targetType)

        targets[targetId] = target

        eventsListener.onTargetCreated(target, context)
    }
}