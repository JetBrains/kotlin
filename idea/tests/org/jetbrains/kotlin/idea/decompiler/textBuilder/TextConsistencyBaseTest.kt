/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.fileClasses.OldPackageFacadeClassUtils
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.junit.Assert

abstract class TextConsistencyBaseTest : KotlinLightCodeInsightFixtureTestCase() {

    protected abstract fun getPackages(): List<FqName>

    protected open fun getFacades(): List<FqName> = emptyList()

    protected abstract fun getTopLevelMembers(): Map<String, String>

    protected abstract fun getVirtualFileFinder(): VirtualFileFinder

    protected abstract fun getDecompiledText(packageFile: VirtualFile, resolver: ResolverForDecompiler? = null): String

    protected abstract fun getModuleDescriptor(): ModuleDescriptor

    protected abstract fun isFromFacade(descriptor: CallableMemberDescriptor, facadeFqName: FqName): Boolean

    fun testConsistency() {
        getPackages().forEach { doTestPackage(it) }
        getFacades().forEach { doTestFacade(it) }
    }

    private fun doTestPackage(packageFqName: FqName) {
        doTestClass(packageFqName, OldPackageFacadeClassUtils.getPackageClassId(packageFqName))
    }

    private fun doTestFacade(facadeFqName: FqName) {
        doTestClass(facadeFqName, ClassId.topLevel(facadeFqName))
    }

    private fun doTestClass(testFqName: FqName, classId: ClassId) {
        val classFile = getVirtualFileFinder().findVirtualFileWithHeader(classId)!!
        val projectBasedText = getDecompiledText(classFile, ResolverForDecompilerImpl(getModuleDescriptor()))
        val deserializedText = getDecompiledText(classFile)
        Assert.assertEquals(projectBasedText, deserializedText)
        // sanity checks
        getTopLevelMembers()[testFqName.asString()]?.let {
            Assert.assertTrue(projectBasedText.contains(it))
        }
        Assert.assertFalse(projectBasedText.contains("ERROR"))
    }

    private inner class ResolverForDecompilerImpl(val module: ModuleDescriptor) : ResolverForDecompiler {
        override fun resolveTopLevelClass(classId: ClassId): ClassDescriptor? =
                module.resolveTopLevelClass(classId.asSingleFqName(), NoLookupLocation.FROM_TEST)

        override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> =
                module.getPackage(facadeFqName.parent()).memberScope.getContributedDescriptors().filter {
                    it is CallableMemberDescriptor &&
                    it.module != module.builtIns.builtInsModule &&
                    isFromFacade(it, facadeFqName)
                }.sortedWith(MemberComparator.INSTANCE)
    }
}

