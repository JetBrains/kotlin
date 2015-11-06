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

package org.jetbrains.kotlin.lang.resolve.android.test

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.android.synthetic.res.AndroidPackageFragmentProviderExtension
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticPackageFragmentProvider
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

public abstract class AbstractAndroidSyntheticPropertyDescriptorTest : UsefulTestCase() {

    public fun doTest(path: String) {
        val config = KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.ANDROID_API)
        val env = createAndroidTestEnvironment(config, getResPaths(path))
        val project = env.project

        val ext = PackageFragmentProviderExtension.getInstances(project).first { it is AndroidPackageFragmentProviderExtension }

        val analysisResult = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(project, listOf(), JvmPackagePartProvider(env))

        val fragmentProvider = ext.getPackageFragmentProvider(project, analysisResult.moduleDescriptor, LockBasedStorageManager.NO_LOCKS,
                                       KotlinTestUtils.DUMMY_EXCEPTION_ON_ERROR_TRACE, null) as AndroidSyntheticPackageFragmentProvider

        val renderer = DescriptorRenderer.COMPACT_WITH_MODIFIERS
        val expected = fragmentProvider.packageFragments.sortedBy { it.fqName.asString() }.map {
            val descriptors = it.getMemberScope().getContributedDescriptors().map { "    " + renderer.render(it) }.joinToString("\n")
            it.fqName.asString() + (if (descriptors.isNotEmpty()) "\n\n" + descriptors else "")
        }.joinToString("\n\n\n")

        KotlinTestUtils.assertEqualsToFile(File(path, "result.txt"), expected)
    }
}