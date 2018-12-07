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

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.junit.Assume

sealed class GradleVersionRequired(val minVersion: String, val maxVersion: String?) {
    companion object {
        //TODO once the plugin is compiled with API level 1.0, replace with the really supported one (3.2)
        // Currently, it will lead to failing tests due to the conflict with the pre-release kotlin-reflect
        // bundled into Gradle 3.2...3.4
        const val OLDEST_SUPPORTED = "3.5"
    }

    class Exact(version: String) : GradleVersionRequired(version, version)

    class AtLeast(version: String) : GradleVersionRequired(version, null)

    class InRange(minVersion: String, maxVersion: String) : GradleVersionRequired(minVersion, maxVersion)

    object None : GradleVersionRequired(GradleVersionRequired.OLDEST_SUPPORTED, null)
}


fun BaseGradleIT.Project.chooseWrapperVersionOrFinishTest(): String {
    val gradleVersionForTests = System.getProperty("kotlin.gradle.version.for.tests")?.toGradleVersion()
    val minVersion = gradleVersionRequirement.minVersion.toGradleVersion()
    val maxVersion = gradleVersionRequirement.maxVersion?.toGradleVersion()

    if (gradleVersionForTests == null) {
        return minVersion.version
    }

    Assume.assumeTrue(minVersion <= gradleVersionForTests && (maxVersion == null || gradleVersionForTests <= maxVersion))

    return gradleVersionForTests.version
}

private fun String.toGradleVersion() = GradleVersion.version(this)