/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.synthetic.test

import org.jetbrains.kotlin.android.synthetic.res.AndroidPackageFragmentProviderExtension
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticPackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.DummyTraces
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

        val fragmentProvider =
            ext.getPackageFragmentProvider(
                project, analysisResult.moduleDescriptor, LockBasedStorageManager.NO_LOCKS,
                DummyTraces.DUMMY_EXCEPTION_ON_ERROR_TRACE, null, LookupTracker.DO_NOTHING
            ) as AndroidSyntheticPackageFragmentProvider

        val renderer = DescriptorRenderer.COMPACT_WITH_MODIFIERS
        val expected = fragmentProvider.packages.values
            .map { it() }
            .sortedBy { it.fqName.asString() }
            .joinToString(separator = "\n\n\n") { packageFragment ->
                val descriptors = packageFragment.getMemberScope().getContributedDescriptors()
                    .sortedWith(MemberComparator.INSTANCE)
                    .joinToString("\n") { "    " + renderer.render(it) }
                packageFragment.fqName.asString() + (if (descriptors.isNotEmpty()) "\n\n" + descriptors else "")
            }

        KotlinTestUtils.assertEqualsToFile(File(path, "result.txt"), expected)
    }
}
