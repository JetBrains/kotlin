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

package org.jetbrains.kotlin.gradle.tasks.incremental.android

import org.jetbrains.kotlin.gradle.tasks.ArtifactDifference
import org.jetbrains.kotlin.gradle.tasks.ArtifactDifferenceRegistry
import java.io.File

// When lib is compiled, changes are associated with .aar files.
// However when app is compiled, there is just .jar in classpath.
internal class ArtifactDifferenceRegistryAndroidWrapper(
        private val registry: ArtifactDifferenceRegistry,
        private val jarToAarMapping: Map<File, File>
): ArtifactDifferenceRegistry by registry {
    override fun get(artifact: File): Iterable<ArtifactDifference>? {
        val mappedFile = jarToAarMapping[artifact] ?: return null
        return registry[mappedFile]
    }
}