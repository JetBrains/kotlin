/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.util.InternalServer
import java.nio.file.Path
import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.util.ProgressCallback
import java.nio.file.Paths

class MingwConfigurablesImpl(target: KonanTarget, properties: Properties, dependenciesRoot: String?, progressCallback : ProgressCallback) : MingwConfigurables,
    KonanPropertiesLoader(target, properties, dependenciesRoot, progressCallback = progressCallback) {
    override val windowsKit: WindowsKit by lazy {
        when (windowsSdkPartsProvider) {
            WindowsSdkPartsProvider.InternalServer -> WindowsKit.CustomPath(Paths.get(absolute(windowsKitParts)))
            WindowsSdkPartsProvider.Local -> WindowsKit.DefaultPath
        }
    }
    override val msvc: Msvc by lazy {
        when (windowsSdkPartsProvider) {
            WindowsSdkPartsProvider.InternalServer -> Msvc.CustomPath(Paths.get(absolute(msvcParts)))
            WindowsSdkPartsProvider.Local -> Msvc.DefaultPath
        }
    }

    private val windowsSdkPartsProvider by lazy {
        if (InternalServer.isAvailable) {
            WindowsSdkPartsProvider.InternalServer
        } else {
            WindowsSdkPartsProvider.Local
        }
    }

    private val windowsHostDependencies by lazy {
        when (windowsSdkPartsProvider) {
            WindowsSdkPartsProvider.InternalServer -> listOf(windowsKitParts, msvcParts)
            WindowsSdkPartsProvider.Local -> emptyList()
        }
    }

    override val dependencies
        get() = super.dependencies + if (HostManager.hostIsMingw) {
            windowsHostDependencies
        } else {
            emptyList()
        }
}

sealed class Msvc {

    abstract fun compilerFlags(): List<String>

    object DefaultPath : Msvc() {
        override fun compilerFlags(): List<String> = emptyList()
    }

    class CustomPath(private val path: Path) : Msvc() {
        override fun compilerFlags(): List<String> = buildList {
            val pathStr = path.toAbsolutePath().toString()
            addAll(listOf("-Xmicrosoft-visualc-tools-root", pathStr))
            add("-Wl,-vctoolsdir:$pathStr")
        }
    }
}

sealed class WindowsKit {
    abstract fun compilerFlags(): List<String>

    object DefaultPath : WindowsKit() {
        override fun compilerFlags(): List<String> = emptyList()
    }

    class CustomPath(private val path: Path) : WindowsKit() {
        override fun compilerFlags(): List<String> = buildList {
            val pathStr = path.toAbsolutePath().toString()
            addAll(listOf("-Xmicrosoft-windows-sdk-root", pathStr))
            add("-Wl,-winsdkdir:$pathStr")

            addAll(listOf("-L", path.resolve("Lib").resolve("um").resolve("x64").toAbsolutePath().toString()))
        }
    }
}

private sealed class WindowsSdkPartsProvider {
    object Local : WindowsSdkPartsProvider()
    object InternalServer : WindowsSdkPartsProvider()
}