/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.process.CommandLineArgumentProvider

/**
 * Passes [testPackages], see [TestDataManagerExtension.testPackages], to the tests of the module, where a generated managed test
 * outside them fails: the test data manager wouldn't find it.
 */
abstract class TestPackagesArgumentProvider : CommandLineArgumentProvider {
    @get:Input
    abstract val testPackages: ListProperty<String>

    override fun asArguments(): Iterable<String> =
        listOf("-D${TestDataManagerOption.TEST_PACKAGES}=${testPackages.get().joinToString(",")}")
}
