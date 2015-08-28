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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.junit.Assert

public abstract class TextConsistencyBaseTest : JetLightCodeInsightFixtureTestCase() {

    protected abstract fun getPackages(): List<FqName>

    protected abstract fun getTopLevelMembers(): Map<String, String>

    protected abstract fun getVirtualFileFinder(): VirtualFileFinder

    protected abstract fun getDecompiledText(packageFile: VirtualFile, resolver: ResolverForDecompiler? = null): String

    protected abstract fun getModuleDescriptor(): ModuleDescriptor

    public fun testConsistency() {
        getPackages().forEach { doTest(it) }
    }

    private fun doTest(packageFqName: FqName) {
        val packageFile = getVirtualFileFinder().findVirtualFileWithHeader(PackageClassUtils.getPackageClassId(packageFqName))!!
        val projectBasedText = getDecompiledText(packageFile, ResolverForDecompilerImpl(getModuleDescriptor()))
        val deserializedText = getDecompiledText(packageFile)
        Assert.assertEquals(projectBasedText, deserializedText)
        // sanity checks
        getTopLevelMembers()[packageFqName.asString()]?.let {
            Assert.assertTrue(projectBasedText.contains(it))
        }
        Assert.assertFalse(projectBasedText.contains("ERROR"))
    }
}

private class ResolverForDecompilerImpl(val module: ModuleDescriptor) : ResolverForDecompiler {
    override fun resolveTopLevelClass(classId: ClassId): ClassDescriptor? {
        return module.resolveTopLevelClass(classId.asSingleFqName(), NoLookupLocation.FROM_TEST)
    }

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): Collection<DeclarationDescriptor> {
        // TODO how to handle non-package facades properly here?
        return module.getPackage(facadeFqName.parent()).memberScope.getAllDescriptors() filter {
            it is CallableMemberDescriptor && it.module != KotlinBuiltIns.getInstance().getBuiltInsModule()
        } sortBy MemberComparator.INSTANCE
    }
}
