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

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.idea.decompiler.classFile.buildDecompiledTextForClassFile
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.isFromJvmPackagePart
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert

class DecompiledTextConsistencyTest : LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() =
            object : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
                override fun getSdk() = PluginTestCaseBase.fullJdk()
            }

    fun testConsistency() {
        for ((packageFacadeFqName, topLevelMembers) in listOf(
                FqName("kotlin.collections.CollectionsKt") to "mutableListOf",
                FqName("kotlin.collections.TypeAliasesKt") to null
        )) {
            val classId = ClassId.topLevel(packageFacadeFqName)
            val classFile = VirtualFileFinder.SERVICE.getInstance(project).findVirtualFileWithHeader(classId)!!

            val module = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    project, listOf(), BindingTraceContext(), KotlinTestUtils.newConfiguration(), ::IDEPackagePartProvider
            ).moduleDescriptor

            val projectBasedText = buildDecompiledTextForClassFile(classFile, ResolverForDecompilerImpl(module)).text
            val deserializedText = buildDecompiledTextForClassFile(classFile).text
            Assert.assertEquals(projectBasedText, deserializedText)

            // sanity checks
            if (topLevelMembers != null) {
                Assert.assertTrue(topLevelMembers in projectBasedText)
            }

            Assert.assertFalse("ERROR" in projectBasedText)
        }
    }

    private inner class ResolverForDecompilerImpl(val module: ModuleDescriptor) : ResolverForDecompiler {
        override fun resolveTopLevelClass(classId: ClassId): ClassDescriptor? =
                module.resolveTopLevelClass(classId.asSingleFqName(), NoLookupLocation.FROM_TEST)

        override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> =
                module.getPackage(facadeFqName.parent()).memberScope.getContributedDescriptors().filter { descriptor ->
                    (descriptor is MemberDescriptor && descriptor !is ClassDescriptor && isFromFacade(descriptor, facadeFqName)) &&
                    !KotlinBuiltIns.isBuiltIn(descriptor)
                }.sortedWith(MemberComparator.INSTANCE)

        private fun isFromFacade(descriptor: MemberDescriptor, facadeFqName: FqName): Boolean =
                descriptor is DeserializedMemberDescriptor &&
                descriptor.isFromJvmPackagePart() &&
                facadeFqName == JvmFileClassUtil.getPartFqNameForDeserialized(descriptor)
    }
}
