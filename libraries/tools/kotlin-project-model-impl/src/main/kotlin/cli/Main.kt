/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.cli

import compiler.Config
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.project.modelx.*
import org.jetbrains.kotlin.project.modelx.compiler.applyKotlinAttribute
import org.jetbrains.kotlin.project.modelx.plainBuildSystem.DefaultKpmFileStructure
import org.jetbrains.kotlin.project.modelx.compiler.PlainCompilerFacade
import org.jetbrains.kotlin.project.modelx.compiler.parseCompilationRequest
import org.jetbrains.kotlin.project.modelx.languageSetting.*
import org.jetbrains.kotlin.project.modelx.serialization.JsonKpmSerializer
import org.jetbrains.kotlin.project.modelx.serialization.JvmTargetSerializer
import org.jetbrains.kotlin.project.modelx.serialization.KotlinModuleDtoTransformer
import org.jetbrains.kotlin.project.modelx.serialization.PlatformsSerializer
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

fun main(args: Array<String>) {
    val kpmFile = Paths.get( args.firstOrNull() ?: "kpm.json")
    if (!kpmFile.exists() || !kpmFile.isRegularFile()) {
        println("kpm.json doesn't not exists or not a file")
        return
    }

    val config = parseCompilationRequest(kpmFile.readText())

    val cliCompilerFacade = createPlainCompilerFacade(Paths.get("."))
    val exitCode = cliCompilerFacade.exec(config)

    println(exitCode)
}

@OptIn(InternalSerializationApi::class)
private fun createPlainCompilerFacade(basePath: Path): PlainCompilerFacade {
    val serializer = JsonKpmSerializer {
        prettyPrint = true
    }

    val kotlinModuleDtoTransformer = KotlinModuleDtoTransformer(
        json = Json { prettyPrint = true }, // todo  reuse from JsonKpmSerializer
        settingSerializers = defaultLanguageSettingsSerializers(),
        attributeSerializers = mapOf(
            "platforms" to (Platforms to PlatformsSerializer),
            "jvm.target" to (JvmTargetAttribute to JvmTargetSerializer)
        )
// TODO: possible alternative
//        attributeSerializers = Attribute::class
//            .sealedSubclasses
//            .associate {
//                val stringKey = it.findAnnotation<AttributeKey>()?.key ?: it.qualifiedName ?: error("Cant get key")
//                val companion = checkNotNull(it.companionObjectInstance)
//
//                val objectKey = companion as? Attribute.Key ?: error("Companion is not Attribute.Key")
//                val serializer = it.serializer()
//
//                stringKey to (objectKey to serializer)
//            }
    )

    val fileStructure = DefaultKpmFileStructure(basePath)

    val config = Config(
        kpmFileStructure = fileStructure,
        attributeContributors = mapOf(
            K2JSCompilerArguments::class.java to { (key, value) -> (this as K2JSCompilerArguments).applyKotlinAttribute(key, value) },
            K2JVMCompilerArguments::class.java to { (key, value) -> (this as K2JVMCompilerArguments).applyKotlinAttribute(key, value) },
        ),
        settingsContributors = defaultLanguageSettingsContributors()
    )

    return PlainCompilerFacade(
        serializer = serializer,
        kpmDtoTransformer = kotlinModuleDtoTransformer,
        compilers = createCliCompilers(System.out),
        config = config
    )
}
