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

import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.name.FqName
import org.junit.Assert
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import com.intellij.psi.search.GlobalSearchScope

public class DecompiledTextConsistencyTest : JetLightCodeInsightFixtureTestCase() {

    private val STANDARD_LIBRARY_FQNAME = FqName("kotlin")

    public fun testConsistencyWithJavaDescriptorResolver() {
        val project = getProject()
        val packageClassFqName = PackageClassUtils.getPackageClassFqName(STANDARD_LIBRARY_FQNAME)
        val virtualFileFinder = VirtualFileFinderFactory.SERVICE.getInstance(project).create(GlobalSearchScope.allScope(project))
        val kotlinPackageFile = virtualFileFinder.findVirtualFileWithHeader(packageClassFqName)!!
        val projectBasedText = buildDecompiledText(kotlinPackageFile, ProjectBasedResolverForDecompiler(project)).text
        val deserializedText = buildDecompiledText(kotlinPackageFile).text
        Assert.assertEquals(projectBasedText, deserializedText)
        // sanity checks
        Assert.assertTrue(projectBasedText.contains("linkedListOf"))
        Assert.assertFalse(projectBasedText.contains("ERROR"))
    }

    override fun getProjectDescriptor() = object : JetWithJdkAndRuntimeLightProjectDescriptor() {
        override fun getSdk() = PluginTestCaseBase.fullJdk()
    }
}

class ProjectBasedResolverForDecompiler(project: Project) : ResolverForDecompiler {
    val module: ModuleDescriptor = run {
        val module = TopDownAnalyzerFacadeForJVM.createJavaModule("<module for resolving stdlib with java top down analysis>")
        module.addDependencyOnModule(module)
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())
        module.seal()
        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(project, GlobalContext(), listOf(), BindingTraceContext(), module, null, null).moduleDescriptor
    }

    override fun resolveTopLevelClass(classId: ClassId): ClassDescriptor? {
        return module.resolveTopLevelClass(classId.asSingleFqName().toSafe())
    }

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        val packageView = module.getPackage(packageFqName) ?: return listOf()
        return packageView.getMemberScope().getAllDescriptors() filter {
            it is CallableMemberDescriptor && it.module != KotlinBuiltIns.getInstance().getBuiltInsModule()
        } sortBy MemberComparator.INSTANCE
    }
}
