/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package  org.jetbrains.kotlin.native.interop.tool

import kotlinx.cinterop.loadKonanLibrary
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.konan.target.AbstractToolConfig
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.Language

class ToolConfig(
        userProvidedTargetName: String?,
        flavor: KotlinPlatform,
        propertyOverrides: Map<String, String>,
        konanDataDir: String? = null,
        val konanHome: String? = null,
) : AbstractToolConfig(konanHome ?: KotlinNativePaths.homePath.absolutePath, userProvidedTargetName, propertyOverrides, konanDataDir) {

    val clang = when (flavor) {
        KotlinPlatform.JVM -> platform.clangForJni
        KotlinPlatform.NATIVE -> platform.clang
    }

    fun getDefaultCompilerOptsForLanguage(language: Language): List<String> = when (language) {
        Language.C,
        Language.OBJECTIVE_C -> clang.libclangArgs.toList()
        Language.CPP -> clang.libclangXXArgs.toList()
    }

    val platformCompilerOpts =
            if (clang is ClangArgs.Jni)
                clang.hostCompilerArgsForJni.toList()
            else emptyList()

    override fun loadLibclang() {
        System.load(libclang)
        // Note: later, in `clangKt.loadLibrary`, `clangstubs` will also be attempted to load from `KotlinNativePaths.homePath`
        // However, that second load will be skipped.
        // The load in `clangKt.loadLibrary` is needed in other usecases, via `clangKt.<clinit>`, before invocation of `clang_...()` funs
        loadKonanLibrary("clangstubs", konanHome)
    }
}
