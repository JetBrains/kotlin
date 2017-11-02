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
import org.junit.AssumptionViolatedException

sealed class GradleVersionRequirement(val version: String) {
    companion object {
        //TODO once the plugin is compiled with API level 1.0, replace with the really supported one (3.2)
        // Currently, it will lead to failing tests due to the conflict with the pre-release kotlin-reflect
        // bundled into Gradle 3.2...3.4
        const val OLDEST_SUPPORTED = "3.5"
    }
}

class SpecificGradleVersion(version: String) : GradleVersionRequirement(version)

class GradleVersionAtLeast(version: String) : GradleVersionRequirement(version)

object NoSpecificGradleVersion : GradleVersionRequirement(GradleVersionRequirement.OLDEST_SUPPORTED)

fun BaseGradleIT.Project.chooseWrapperVersionOrFinishTest(): String {
    val advanceGradleVersionArg: String? = System.getProperty("advanceGradleVersion")
    val requiredVersion = gradleVersionRequirement.version

    /* Given arg = advanceGradleVersionArg,
             req = requiredVersion,
             min = GradleVersionRequirement.OLDEST_SUPPORTED

       Decide whether to run the test (and with which version) or ignore it as follows:

       project.requiredGradleVersion  | arg is null |  arg < req  |  arg = req  |  arg > req  |
       -------------------------------|-------------|-------------|-------------|-------------|
       SpecificGradleVersion(req)     |  RUN (req)  |   IGNORE    |    IGNORE   |   IGNORE    |
       GradleVersionAtLeast(req)      |  RUN (req)  |   IGNORE    |     RUN     |  RUN (arg)  |
       NoSpecificVersion (req is min) |  RUN (min)  |   IGNORE    |     RUN     |  RUN (arg)  |
     */

    return when {
        advanceGradleVersionArg == null -> requiredVersion
        else -> {
            if (gradleVersionRequirement is SpecificGradleVersion)
                throw AssumptionViolatedException("This test is ignored as it requires a specific Gradle version $requiredVersion. " +
                                                  "Remove the advanceGradleVersion system property to run it.")

            val argComparedToReq = gradleVersionsComparator.compare(advanceGradleVersionArg, requiredVersion)

            when {
                argComparedToReq < 0 -> throw AssumptionViolatedException(
                        "The test requires Gradle version $requiredVersion, but this run uses $advanceGradleVersionArg")
                argComparedToReq == 0 -> advanceGradleVersionArg
                else /* argComparedToReq > 0 */ ->
                    if (gradleVersionRequirement is SpecificGradleVersion)
                        throw AssumptionViolatedException(
                                "The test requires specific Gradle version $requiredVersion and is not subject to " +
                                "version advance to $advanceGradleVersionArg")
                    else advanceGradleVersionArg
            }
        }
    }
}

private val gradleVersionsComparator = compareBy<String> { GradleVersion.version(it) }
