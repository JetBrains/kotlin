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

package org.jetbrains.kotlin.android.synthetic.test

import org.jetbrains.kotlin.android.synthetic.res.AndroidPackageFragmentProviderExtension
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticPackageFragmentProvider
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

abstract class AbstractAndroidSyntheticPropertyDescriptorTest : KtUsefulTestCase() {
    fun doTest(path: String) {
        val config = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.ANDROID_API)
        val env = createTestEnvironment(config, getResPaths(path))
        val project = env.project

        val ext = PackageFragmentProviderExtension.getInstances(project).first { it is AndroidPackageFragmentProviderExtension }

        val analysisResult = JvmResolveUtil.analyzeAndCheckForErrors(listOf(), env)

        val fragmentProvider = ext.getPackageFragmentProvider(project, analysisResult.moduleDescriptor, LockBasedStorageManager.NO_LOCKS,
                                       KotlinTestUtils.DUMMY_EXCEPTION_ON_ERROR_TRACE, null) as AndroidSyntheticPackageFragmentProvider

        val renderer = DescriptorRenderer.COMPACT_WITH_MODIFIERS
        val expected = fragmentProvider.packageFragments.sortedBy { it.fqName.asString() }.map {
            val descriptors = it.getMemberScope().getContributedDescriptors()
                    .sortedWith(MemberComparator.INSTANCE)
                    .map { "    " + renderer.render(it) }.joinToString("\n")
            it.fqName.asString() + (if (descriptors.isNotEmpty()) "\n\n" + descriptors else "")
        }.joinToString("\n\n\n")

        KotlinTestUtils.assertEqualsToFile(File(path, "result.txt"), expected)
    }
}
