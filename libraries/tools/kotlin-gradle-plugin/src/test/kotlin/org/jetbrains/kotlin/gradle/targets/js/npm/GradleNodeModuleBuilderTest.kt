/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull

class GradleNodeModuleBuilderTest {

    // Gson (used in fromSrcPackageJson) deserialize json to PackageJson no matter on nullability and default values
    //  Check that in case where there is no dependencies fields, we don't get nullable fields, that declared as non-nullable
    @Test
    fun validPackageJsonWithoutDependencies() {
        val packageJson = fromSrcPackageJson(
            File("libraries/tools/kotlin-gradle-plugin/src/test/resources/org/jetbrains/kotlin/gradle/targets/js/npm/GradleNodeModuleBuilderTest/package.json")
        )
        assertNotNull(packageJson, "package.json should be deserialized")

        with(packageJson!!) {
            listOf(
                dependencies,
                devDependencies,
                peerDependencies,
                optionalDependencies,
                bundledDependencies
            ).forEach {
                assertNotNull(it, "Dependencies should deserialized correctly without null")
            }
        }
    }
}