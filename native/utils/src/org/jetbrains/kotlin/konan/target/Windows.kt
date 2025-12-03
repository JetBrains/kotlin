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
            WindowsSdkPartsProvider.InternalServer -> WindowsKit.CustomPath(
                Paths.get(absolute(windowsKitParts)),
                target
            )
            WindowsSdkPartsProvider.Local -> WindowsKit.DefaultPath
        }
    }
    override val msvc: Msvc by lazy {
        when (windowsSdkPartsProvider) {
            WindowsSdkPartsProvider.InternalServer -> Msvc.CustomPath(
                Paths.get(absolute(msvcParts))
            )
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
        get() = super.dependencies + windowsHostDependencies
}

sealed class Msvc {

    abstract fun compilerFlags(): List<String>

    object DefaultPath : Msvc() {
        override fun compilerFlags(): List<String> = emptyList()
    }

    class CustomPath(private val msvcParts: Path) : Msvc() {
        override fun compilerFlags(): List<String> = buildList {
            /*
            By default, Clang would try to find a local installation of MSVC and add its contents
            to library and header search paths, e.g.
            * https://github.com/llvm/llvm-project/blob/32cd703da578e769787a921d76b768164a4256b6/clang/lib/Driver/ToolChains/MSVC.cpp#L445-L455
            * https://github.com/llvm/llvm-project/blob/32cd703da578e769787a921d76b768164a4256b6/clang/lib/Driver/ToolChains/MSVC.cpp#L112-L121
            * https://github.com/llvm/llvm-project/blob/32cd703da578e769787a921d76b768164a4256b6/clang/lib/Driver/ToolChains/MSVC.cpp#L700-L705

            Since the goal of `CustomPath` is to make Clang work independently of any local MSVC
            installations, this is a problem.

            The easiest way to disable that behavior is to pass `-Xmicrosoft-visualc-tools-root`.
            The flag is undocumented and is not widely known, but it is basically equivalent to
            `/vctoolsdir` (available when using an alternative Clang driver for Windows, `clang-cl`):
            * https://clang.llvm.org/docs/UsersManual.html#windows-system-headers-and-library-lookup
            * https://github.com/llvm/llvm-project/blob/8ebbcfb4e326abb71e80fad05951e2e74ae7f64a/clang/include/clang/Driver/Options.td#L8967

            For Kotlin, it is easier to use the same Clang driver for all targets, hence the need
            for the undocumented flag.

            `msvcParts` is passed as the flag value. Technically, it is not an MSVC installation
            (while the flag expects one), but the layout of the important parts matches, so this
            also makes Clang automatically add the directories within `msvcParts` as header and
            library search paths.

            `/vctoolsversion` is not passed, but it doesn't seem to have any effect here.
             */
            val pathStr = msvcParts.toAbsolutePath().toString()
            addAll(listOf("-Xmicrosoft-visualc-tools-root", pathStr))

            /*
            Apparently, the linker (`lld-link`) also has the same kind of behavior (adding local
            MSVC paths automatically), despite having the same paths passed from the compiler driver:
            https://github.com/llvm/llvm-project/blob/615b7eeaa94d7c2d2c782fcdc21de5b62f3c168e/lld/COFF/Driver.cpp#L665-L669

            Workaround is basically the same: pass `-vctoolsdir` to the linker.
             */
            add("-Wl,-vctoolsdir:$pathStr")
        }
    }
}

sealed class WindowsKit {
    abstract fun compilerFlags(): List<String>

    object DefaultPath : WindowsKit() {
        override fun compilerFlags(): List<String> = emptyList()
    }

    class CustomPath(private val windowsKitParts: Path, val target: KonanTarget) : WindowsKit() {
        override fun compilerFlags(): List<String> = buildList {
            /*
            See the comments in [MSVC.CustomPath.compilerFlags].

            Additional source code references:
            * https://github.com/llvm/llvm-project/blob/32cd703da578e769787a921d76b768164a4256b6/clang/lib/Driver/ToolChains/MSVC.cpp#L125-L134
            * https://github.com/llvm/llvm-project/blob/32cd703da578e769787a921d76b768164a4256b6/clang/lib/Driver/ToolChains/MSVC.cpp#L707-L718
            * https://github.com/llvm/llvm-project/blob/8ebbcfb4e326abb71e80fad05951e2e74ae7f64a/clang/include/clang/Driver/Options.td#L8970
            * https://github.com/llvm/llvm-project/blob/615b7eeaa94d7c2d2c782fcdc21de5b62f3c168e/lld/COFF/Driver.cpp#L671-L683

            clang's `-Xmicrosoft-windows-sdk-root` is equivalent to clang-cl's `/winsdkdir`.
             */
            val pathStr = windowsKitParts.toAbsolutePath().toString()
            addAll(listOf("-Xmicrosoft-windows-sdk-root", pathStr))
            add("-Wl,-winsdkdir:$pathStr")

            /*
            Unlike with MSVC, the layout of `windowsKitParts` doesn't match the layout of the expected
            Windows SDK root. This problem is tracked in KT-83978.
            `windowsKitParts` has:
            * `Include/ucrt`
            * `Lib/ucrt/$arch`
            * `Lib/um/$arch`

            Windows SDK has instead:
            * `Include/$version/ucrt`
            * `Lib/$version/ucrt/$arch`
            * `Lib/$version/um/$arch`

            By not passing the SDK version, this code tricks Clang into omitting the version from the paths.
            But then it also omits `um` from the lib path:
            https://github.com/llvm/llvm-project/blob/615b7eeaa94d7c2d2c782fcdc21de5b62f3c168e/lld/COFF/Driver.cpp#L629-L630

            As a simple workaround, just pass the proper path explicitly:
             */
            val arch = when (target) {
                KonanTarget.MINGW_X64 -> "x64"
                else -> error("WindowsKit.CustomPath supports only mingw_x64 for now.")
            }
            val libUmPath = windowsKitParts.resolve("Lib").resolve("um").resolve(arch)
            addAll(listOf("-L", libUmPath.toAbsolutePath().toString()))
        }
    }
}

private sealed class WindowsSdkPartsProvider {
    object Local : WindowsSdkPartsProvider()
    object InternalServer : WindowsSdkPartsProvider()
}