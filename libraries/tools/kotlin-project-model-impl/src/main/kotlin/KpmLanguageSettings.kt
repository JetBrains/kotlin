/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

import kotlinx.serialization.*
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.jetbrains.kotlin.config.JvmStringConcat
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.load.java.Jsr305Settings
import org.jetbrains.kotlin.load.java.ReportLevel
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.project.modelx.serialization.ToStringSerializer

@SerialInfo
@Target(AnnotationTarget.CLASS)
annotation class CompilerSettingKey(
    val name: String
)

@Target(AnnotationTarget.CLASS)
annotation class CompilerSettingRelation(
    val type: RelationType
)

/**
 * Given A, B are KPM [Fragment]
 * such that A refines B
 * And lsA and lsB are language settings of A and B accordingly of same [LanguageSetting] type.
 *
 * See [Compiler setting consistency](https://helpserver.labs.jb.gg/help/kotlin-project-model/core-notions.html#compiler-settings-consistency)
 */
enum class RelationType {
    /**
     * there is no consistency check between lsA and lsB thus they are always consistent
     */
    DONT_CARE,

    /**
     * lsA >= lsB
     */
    MONOTONIC_ASC,

    /**
     * lsA <= lsB
     */
    MONOTONIC_DSC,

    /**
     * lsA == lsB
     */
    CONSTANT
}

sealed interface LanguageSetting
sealed interface JvmLanguageSetting : LanguageSetting
sealed interface JSLanguageSetting : LanguageSetting

@CompilerSettingKey("jvm.string-concat")
@CompilerSettingRelation(RelationType.DONT_CARE)
@JvmInline
value class JvmStringConcatSetting(
    val value: JvmStringConcat
) : JvmLanguageSetting {
    companion object: ToStringSerializer<JvmStringConcatSetting>(
        serialName = "jvm.string-concat",
        getter = { value.description },
        constructor = { JvmStringConcat.fromString(it)?.let(::JvmStringConcatSetting) ?: error("unknown string-concat: $it") },
    )
}

@CompilerSettingKey("language-version")
@CompilerSettingRelation(RelationType.MONOTONIC_ASC)
@JvmInline
value class LanguageVersionSetting(
    val value: LanguageVersion
): LanguageSetting {
    companion object : ToStringSerializer<LanguageVersionSetting>(
        serialName = "language-version",
        getter = { value.description },
        constructor = { LanguageVersion.fromVersionString(it)?.let(::LanguageVersionSetting) ?: error("Unexpected JvmStringConcat value") }
    )
}

@CompilerSettingKey("jvm.jsr305")
@CompilerSettingRelation(RelationType.CONSTANT)
@SerialName("jvm.jsr305")
data class Jsr305Setting(
    val value: Jsr305Settings
) : JvmLanguageSetting {
    companion object : KSerializer<Jsr305Setting> {
        override val descriptor = buildClassSerialDescriptor("jvm.jsr305") {
            element<String>("globalLevel")
            element<String?>("migrationLevel")
            element<Map<String, String>>("annotations")
        }

        override fun deserialize(decoder: Decoder): Jsr305Setting {
            var globalLevel: String? = null
            var migrationLevel: String? = null
            var annotations: Map<String, String>? = null

            decoder.decodeStructure(descriptor) {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> globalLevel = decodeStringElement(descriptor, 0)
                        1 -> migrationLevel = decodeNullableSerializableElement(descriptor, 1, String.serializer().nullable)
                        2 -> annotations = decodeSerializableElement(descriptor, 2, serializer())
                        CompositeDecoder.DECODE_DONE -> break
                    }
                }
            }

            val mappedAnnotations = checkNotNull(annotations) { "${descriptor.getElementName(2)} is not present" }
                .entries
                .associate { (fqn, level) -> FqName(fqn) to (ReportLevel.findByDescription(level) ?: error("Unknown level: $level")) }

            return Jsr305Setting( // 🙈🙈🙈 it should be renamed to something else, I guess
                Jsr305Settings(
                    globalLevel = ReportLevel.findByDescription(globalLevel) ?: error("No globalLevel item found"),
                    migrationLevel = ReportLevel.findByDescription(migrationLevel),
                    userDefinedLevelForSpecificAnnotation = mappedAnnotations
                )
            )
        }

        override fun serialize(encoder: Encoder, value: Jsr305Setting) {
            val jsr305 = value.value
            val annotations = jsr305
                .userDefinedLevelForSpecificAnnotation
                .entries
                .associate { (fqn, level) -> fqn.asString() to level.description }


            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, jsr305.globalLevel.description)
                encodeNullableSerializableElement(descriptor, 1, String.serializer(), jsr305.migrationLevel?.description)
                encodeSerializableElement(descriptor, 2, serializer(), annotations)
            }
        }
    }
}
