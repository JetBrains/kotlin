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
import org.jetbrains.kotlin.asJava.OldPackageFacadeClassUtils
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.decompiler.js.buildDecompiledTextFromJsMetadata
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor
import org.jetbrains.kotlin.idea.vfilefinder.JsVirtualFileFinder
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.sure

class KotlinJavaScriptDecompiledTextConsistencyTest : TextConsistencyBaseTest() {
    override fun getPackages(): List<FqName> = listOf(
            "java.util", "jquery", "jquery.ui", "kotlin", "kotlin.browser", "kotlin.dom", "kotlin.js"
    ).map { FqName(it) }

    override fun getTopLevelMembers(): Map<String, String> = mapOf("kotlin" to "intArrayOf")

    override fun getVirtualFileFinder(): VirtualFileFinder = JsVirtualFileFinder.SERVICE.getInstance(project)

    override fun getDecompiledText(packageFile: VirtualFile, resolver: ResolverForDecompiler?): String =
            (resolver?.let { buildDecompiledTextFromJsMetadata(packageFile, it) } ?: buildDecompiledTextFromJsMetadata(packageFile)).text

    override fun getModuleDescriptor(): ModuleDescriptor {
        val stdlibJar = PathUtil.getKotlinPathsForDistDirectory().jsStdLibJarPath.absolutePath
        val module = KotlinTestUtils.createEmptyModule("<module for stdlib>", JsPlatform, JsPlatform.builtIns)
        val metadata = KotlinJavascriptMetadataUtils.loadMetadata(stdlibJar)
        assert(metadata.size == 1)

        val provider = KotlinJavascriptSerializationUtil.readModule(metadata[0].body, LockBasedStorageManager(), module).data
                .sure { "No package fragment provider was created" }

        module.initialize(provider)
        module.setDependencies(module, module.builtIns.builtInsModule)

        return module
    }

    override fun getProjectDescriptor() = KotlinStdJSProjectDescriptor.instance

    override fun isFromFacade(descriptor: CallableMemberDescriptor, facadeFqName: FqName): Boolean {
        val containingDeclaration = descriptor.containingDeclaration
        return containingDeclaration is PackageFragmentDescriptor &&
               facadeFqName == OldPackageFacadeClassUtils.getPackageClassFqName(containingDeclaration.fqName)
    }
}
