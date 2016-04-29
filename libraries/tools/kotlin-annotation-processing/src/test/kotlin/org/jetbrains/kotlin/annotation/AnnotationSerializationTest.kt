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

import com.google.common.io.Files
import java.io.File

class AnnotationSerializationTest : AnnotationListParseTest() {
    // after reading and serializing annotations file should not change the result of AnnotationListParseTest
    override fun doTest(testDir: File) {
        val workingDir = Files.createTempDir()
        testDir.copyRecursively(workingDir)

        val annotationsFile = File(workingDir, ANNOTATIONS_FILE_NAME)
        val annotationProvider = KotlinAnnotationProvider(annotationsFile)
        annotationsFile.delete()

        annotationsFile.bufferedWriter().use { writer ->
            val annotationWriter = CompactAnnotationWriter(writer)
            annotationProvider.writeAnnotations(annotationWriter)
        }

        super.doTest(workingDir)
    }
}