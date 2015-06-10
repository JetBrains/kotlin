/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.js.KotlinJavaScriptLibraryManager
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor
import org.jetbrains.kotlin.idea.vfilefinder.JsVirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.sure
import kotlin.properties.Delegates

public class KotlinJavaScriptDecompiledTextConsistencyTest : TextConsistencyBaseTest() {

    override val packages: List<FqName> = listOf(
            "java.util", "jquery", "jquery.ui", "kotlin", "kotlin.browser", "kotlin.dom", "kotlin.js"
    ).map { FqName(it) }

    override val topLevelMembers: Map<String, String> = mapOf("kotlin" to "intArrayOf")

    override val virtualFileFinder: VirtualFileFinder by Delegates.lazy() {
        JsVirtualFileFinder.SERVICE.getInstance(getProject())
    }

    override fun setUp() {
        super.setUp()
        KotlinJavaScriptLibraryManager.getInstance(getProject()).syncUpdateProjectLibrary()
    }

    override fun getDecompiledText(packageFile: VirtualFile, resolver: ResolverForDecompiler?): String =
            (resolver?.let { buildDecompiledTextFromJsMetadata(packageFile, it) } ?: buildDecompiledTextFromJsMetadata(packageFile)).text

    override fun getModuleDescriptor(): ModuleDescriptor {
        val stdlibJar = PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath().getAbsolutePath()
        val module = JetTestUtils.createEmptyModule("<module for stdlib>")
        val metadata = KotlinJavascriptMetadataUtils.loadMetadata(stdlibJar)
        assert(metadata.size() == 1)

        val provider = KotlinJavascriptSerializationUtil.createPackageFragmentProvider(module, metadata[0].body, LockBasedStorageManager())
                .sure { "No package fragment provider was created" }

        module.initialize(provider)
        module.setDependencies(module, KotlinBuiltIns.getInstance().getBuiltInsModule())

        return module
    }

    override fun getProjectDescriptor() = KotlinStdJSProjectDescriptor.instance
}
