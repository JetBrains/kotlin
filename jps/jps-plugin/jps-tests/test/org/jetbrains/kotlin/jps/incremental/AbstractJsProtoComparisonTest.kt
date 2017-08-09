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

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.Difference
import org.jetbrains.kotlin.incremental.difference as differenceImpl
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.ProtoData
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.junit.Assert
import java.io.File

abstract class AbstractJsProtoComparisonTest : AbstractProtoComparisonTest<ProtoData>() {
    override fun expectedOutputFile(testDir: File): File =
        File(testDir, "result-js.out")
                .takeIf { it.exists() }
                ?: super.expectedOutputFile(testDir)

    override fun compileAndGetClasses(sourceDir: File, outputDir: File): Map<ClassId, ProtoData> {
        val incrementalResults = IncrementalResultsConsumerImpl()
        val services = Services.Builder().run {
            register(IncrementalResultsConsumer::class.java, incrementalResults)
            build()
        }

        val ktFiles = sourceDir.walkMatching { it.name.endsWith(".kt") }.map { it.canonicalPath }.toList()
        val messageCollector = TestMessageCollector()
        val outputItemsCollector = OutputItemsCollectorImpl()
        val args = K2JSCompilerArguments().apply {
            outputFile = File(outputDir, "out.js").canonicalPath
            metaInfo = true
            main = K2JsArgumentConstants.NO_CALL
            freeArgs.addAll(ktFiles)
        }

        val env = createTestingCompilerEnvironment(messageCollector, outputItemsCollector, services)
        runJSCompiler(args, env).let { exitCode ->
            val expectedOutput = "OK"
            val actualOutput = (listOf(exitCode?.name) + messageCollector.errors).joinToString("\n")
            Assert.assertEquals(expectedOutput, actualOutput)
        }

        val classes = hashMapOf<ClassId, ProtoData>()

        for ((sourceFile, protoBytes, _) in incrementalResults.packageParts) {
            val proto = ProtoBuf.PackageFragment.parseFrom(protoBytes, JsSerializerProtocol.extensionRegistry)
            val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

            proto.class_List.forEach {
                val classId = nameResolver.getClassId(it.fqName)
                classes[classId] = ClassProtoData(it, nameResolver)
            }

            proto.`package`.apply {
                val packageFqName = if (hasExtension(JsProtoBuf.packageFqName)) {
                    nameResolver.getPackageFqName(getExtension(JsProtoBuf.packageFqName))
                }
                else FqName.ROOT

                val packagePartClassId = ClassId(packageFqName, Name.identifier(sourceFile.nameWithoutExtension.capitalize() + "Kt"))
                classes[packagePartClassId] = PackagePartProtoData(this, nameResolver)
            }
        }

        return classes
    }

    override fun difference(oldData: ProtoData, newData: ProtoData): Difference? =
            differenceImpl(oldData, newData)
}
