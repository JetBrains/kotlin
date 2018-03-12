package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

fun defaultTargetSubstitutions(target: KonanTarget) =
        mapOf<String, String>(
            "target" to target.visibleName,
            "arch" to target.architecture.visibleName,
            "family" to target.family.visibleName)

fun DefFile(file: File?, target: KonanTarget) = DefFile(file, defaultTargetSubstitutions(target))