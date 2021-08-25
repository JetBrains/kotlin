/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.kotlin.project.modelx.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

@Serializable
data class KotlinModuleDto(
    val id: String,
    val fragments: List<FragmentDto>,
    val refinement: Map<FragmentId, Set<FragmentId>>
)

@Serializable
sealed class FragmentDto {
    abstract val id: String
    abstract val settings: Map<String, JsonElement>
    abstract val attributes: Map<String, JsonElement>
    abstract val moduleDependencies: List<String>
}

@Serializable
data class CommonFragmentDto(
    override val id: String,
    override val settings: Map<String, JsonElement>,
    override val attributes: Map<String, JsonElement>,
    override val moduleDependencies: List<String>
) : FragmentDto()

@Serializable
data class VariantDto(
    override val id: String,
    override val settings: Map<String, JsonElement>,
    override val attributes: Map<String, JsonElement>,
    override val moduleDependencies: List<String>,
    val platform: String
) : FragmentDto()

@Serializable
data class KpmVariantInfo(
    val moduleId: ModuleId,
    val variant: VariantDto
)

class KotlinModuleDtoTransformer(
    private val json: Json,
    private val settingSerializers: Map<String, KSerializer<out LanguageSetting>>,
    private val attributeSerializers: Map<String, Pair<Attribute.Key, KSerializer<out Attribute>>>,
) {
    private val attributeSerializersByKey by lazy {
        attributeSerializers.entries.associate { (stringKey, attribute) -> attribute.first to (stringKey to attribute.second) }
    }

    fun toDto(module: KotlinModule): KotlinModuleDto {
        return KotlinModuleDto(
            id = module.id,
            fragments = module.fragments.map { it.value.toDto() },
            refinement = module.refinements
        )
    }

    private fun Fragment.toDto(): FragmentDto {
        val settingsDto = settings
            .mapValues { (key, languageSetting) ->
                val serializer = settingSerializers[key] ?: error("Unknown settings key: $key")
                serializer as SerializationStrategy<LanguageSetting>
                json.encodeToJsonElement(serializer, languageSetting)
            }

        return when(this) {
            is CommonFragment -> CommonFragmentDto(
                id = id,
                settings = settingsDto,
                attributes = emptyMap(),
                moduleDependencies = moduleDependencies.toList(),
            )
            is Variant -> {
                val attributesDto = attributes
                    .entries
                    .associate { (key, attribute) ->
                        val (stringKey, serializer) = attributeSerializersByKey[key] ?: error("Unknown attribute key: $key")
                        serializer as SerializationStrategy<Attribute>
                        stringKey to json.encodeToJsonElement(serializer, attribute)
                    }

                VariantDto(
                    id = id,
                    settings = settingsDto,
                    attributes = attributesDto,
                    moduleDependencies = moduleDependencies.toList(),
                    platform = platform.name
                )
            }
        }
    }

    fun fromDto(moduleDto: KotlinModuleDto): KotlinModule {
        return KotlinModule(
            id = moduleDto.id,
            fragments = moduleDto.fragments.associate { it.id to transform(it) },
            refinements = moduleDto.refinement
        )
    }

    private fun transform(fragmentDto: FragmentDto): Fragment {
        val moduleDependencies = fragmentDto
            .moduleDependencies
            .toSet()

        val settings = fragmentDto
            .settings
            .mapValues { (key, jsonValue) ->
                val serializer = settingSerializers[key] ?: error("Unknown settings key: $key")
                json.decodeFromJsonElement(serializer, jsonValue)
            }

        val attributes = fragmentDto
            .attributes
            .entries
            .associate { (key, jsonValue) ->
                val (attributeKey, serializer) = attributeSerializers[key] ?: error("Unknown attribute key: $key")
                attributeKey to json.decodeFromJsonElement(serializer, jsonValue)
            }

        return when(fragmentDto) {
            is CommonFragmentDto -> CommonFragment(
                id = fragmentDto.id,
                settings = settings,
                moduleDependencies = moduleDependencies,
            )
            is VariantDto -> Variant(
                id = fragmentDto.id,
                settings = settings,
                attributes = attributes,
                moduleDependencies = moduleDependencies,
                // platform = Platform.valueOf(fragmentDto.platform)
            )
        }
    }
}