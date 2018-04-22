package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

fun DefFile(file: File?, target: KonanTarget) = DefFile(file, defaultTargetSubstitutions(target))
