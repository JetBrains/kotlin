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

package org.jetbrains.kotlin.annotation

import org.junit.Test
import java.io.File
import java.io.StringWriter

class MutableAnnotationProviderTest {
    private val resourcesRootFile = File("src/test/resources/mutate")

    @Test
    fun testRemoveClass() {
        val testDir = File(resourcesRootFile, "removeClass")
        val annotationsFile = File(testDir, "annotations.txt")
        val annotationsFileUpdated = File(testDir, "annotations-updated.txt")

        val content = mutateAnnotationsFiles {
            addAnnotationsFrom(annotationsFile)
            removeClasses(setOf("foo.A"))
        }

        assertEqualsToFile(annotationsFileUpdated, content)
    }

    @Test
    fun testMergeFiles() {
        val testDir = File(resourcesRootFile, "mergeFiles")
        val annotationsAFile = File(testDir, "annotationsA.txt")
        val annotationsBFile = File(testDir, "annotationsB.txt")
        val annotationsMergedFile = File(testDir, "annotations-merged.txt")

        val content = mutateAnnotationsFiles {
            addAnnotationsFrom(annotationsAFile)
            addAnnotationsFrom(annotationsBFile)
        }

        assertEqualsToFile(annotationsMergedFile, content)
    }

    private fun mutateAnnotationsFiles(fn: MutableKotlinAnnotationProvider.() -> Unit): String {
        val annotationProvider = MutableKotlinAnnotationProvider()
        annotationProvider.fn()

        val writer = StringWriter()
        annotationProvider.writeAnnotations(CompactAnnotationWriter(writer))
        return writer.toString()
    }
}