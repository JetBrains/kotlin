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

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.incremental.ProtoData
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.incremental.toProtoData
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.test.MockLibraryUtil
import java.io.File

abstract class AbstractJvmProtoComparisonTest : AbstractProtoComparisonTest<LocalFileKotlinClass>() {
    override fun compileAndGetClasses(sourceDir: File, outputDir: File): Map<ClassId, LocalFileKotlinClass> {
        MockLibraryUtil.compileKotlin(sourceDir.path, outputDir, extraOptions = listOf("-Xdisable-default-scripting-plugin"))

        val classFiles = outputDir.walkMatching { it.name.endsWith(".class") }
        val localClassFiles = classFiles.map { LocalFileKotlinClass.create(it)!! }
        return localClassFiles.associateBy { it.classId }
    }

    override fun LocalFileKotlinClass.toProtoData(): ProtoData? {
        assert(classHeader.metadataVersion.isCompatible()) { "Incompatible class ($classHeader): $location" }

        val bytes by lazy { BitEncoding.decodeBytes(classHeader.data!!) }
        val strings by lazy { classHeader.strings!! }
        val packageFqName = classId.packageFqName

        return when (classHeader.kind) {
            KotlinClassHeader.Kind.CLASS -> {
                ProtoMapValue(false, bytes, strings).toProtoData(packageFqName)
            }
            KotlinClassHeader.Kind.FILE_FACADE,
            KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                ProtoMapValue(true, bytes, strings).toProtoData(packageFqName)
            }
            else -> {
                null
            }
        }
    }
}