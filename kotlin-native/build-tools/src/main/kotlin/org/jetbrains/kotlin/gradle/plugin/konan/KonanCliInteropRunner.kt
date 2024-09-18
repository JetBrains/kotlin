/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.konan.target.AbstractToolConfig
import org.jetbrains.kotlin.konan.target.KonanTarget

private val load0 = Runtime::class.java.getDeclaredMethod("load0", Class::class.java, String::class.java).also {
    it.isAccessible = true
}

private class CliToolConfig(konanHome: String, target: String) : AbstractToolConfig(konanHome, target, emptyMap()) {
    override fun loadLibclang() {
        // Load libclang into the system class loader. This is needed to allow developers to make changes
        // in the tooling infrastructure without having to stop the daemon (otherwise libclang might end up
        // loaded in two different class loaders which is not allowed by the JVM).
        load0.invoke(Runtime.getRuntime(), String::class.java, libclang)
    }
}

/** Kotlin/Native C-interop tool runner */
internal class KonanCliInteropRunner(
        fileOperations: FileOperations,
        logger: Logger,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        konanHome: String,
        target: KonanTarget,
) : KonanCliRunner("cinterop", fileOperations, logger, isolatedClassLoadersService, konanHome) {
    init {
        CliToolConfig(konanHome, target.visibleName).prepare()
    }
}