/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModuleType
import java.io.File
import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Type

internal object SerializationTools {

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeHierarchyAdapter(File::class.java, FileTypeAdapter)
            .registerTypeAdapter(GradleSwiftExportModule::class.java, SwiftExportModuleDeserializerAndSerializer)
            .addDeserializationExclusionStrategy(SuperclassExclusionStrategy)
            .addSerializationExclusionStrategy(SuperclassExclusionStrategy)
            .setPrettyPrinting()
            .create()
    }

    inline fun <reified T : List<Serializable>> writeToJson(objects: T): String = gson.toJson(objects)

    inline fun <reified T : List<Serializable>> readFromJson(jsonString: String): T {
        val listType = object : TypeToken<T>() {}.type
        return gson.fromJson(jsonString, listType)
    }
}

private object FileTypeAdapter : JsonSerializer<File>, JsonDeserializer<File> {
    override fun serialize(src: File?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.path)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): File {
        return File(json?.asString.orEmpty())
    }
}

private object SuperclassExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipField(fieldAttributes: FieldAttributes?): Boolean {
        if (fieldAttributes == null) return false
        val fieldName = fieldAttributes.name
        val theClass = fieldAttributes.declaringClass
        return isFieldInSuperclass(theClass, fieldName)
    }

    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
        return false
    }

    private fun isFieldInSuperclass(subclass: Class<*>, fieldName: String): Boolean {
        var superclass = subclass.superclass
        var field: Field?

        while (superclass != null) {
            field = getField(superclass, fieldName)
            if (field != null) {
                return true
            }

            superclass = superclass.superclass
        }

        return false
    }

    private fun getField(theClass: Class<*>, fieldName: String): Field? {
        return try {
            theClass.getDeclaredField(fieldName)
        } catch (e: Exception) {
            null
        }
    }
}

private object SwiftExportModuleDeserializerAndSerializer : JsonDeserializer<GradleSwiftExportModule>,
    JsonSerializer<GradleSwiftExportModule> {
    private val NAME_FIELD = GradleSwiftExportModule::name.name
    private val TYPE_FIELD = GradleSwiftExportModule::type.name
    private val DEPENDENCIES_FIELD = GradleSwiftExportModule::dependencies.name
    private val FILES_FIELD = GradleSwiftExportModule.BridgesToKotlin::files.name
    private val BRIDGE_NAME_FIELD = GradleSwiftExportModule.BridgesToKotlin::bridgeName.name
    private val SWIFT_API_FIELD = GradleSwiftExportModule.SwiftOnly::swiftApi.name

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): GradleSwiftExportModule {
        val jsonObject = json?.asJsonObject ?: throw JsonParseException("Invalid JSON for SwiftExportModule")
        if (context == null) throw JsonParseException("Context is required for deserializing")

        val name = jsonObject.get(NAME_FIELD).asString
        val type = context.deserialize<GradleSwiftExportModuleType>(
            jsonObject.get(TYPE_FIELD),
            GradleSwiftExportModuleType::class.java
        )
        val dependencies = context.deserialize<List<String>>(
            jsonObject.get(DEPENDENCIES_FIELD),
            object : TypeToken<List<String>>() {}.type
        )

        return when (type) {
            GradleSwiftExportModuleType.SWIFT_ONLY -> {
                val swiftApi = context.deserialize<File>(
                    jsonObject.get(SWIFT_API_FIELD),
                    File::class.java
                )

                GradleSwiftExportModule.SwiftOnly(swiftApi, name, dependencies)
            }
            GradleSwiftExportModuleType.BRIDGES_TO_KOTLIN -> {
                val files = context.deserialize<GradleSwiftExportFiles>(
                    jsonObject.get(FILES_FIELD),
                    GradleSwiftExportFiles::class.java
                )

                val bridgeName = jsonObject.get(BRIDGE_NAME_FIELD).asString
                GradleSwiftExportModule.BridgesToKotlin(files, bridgeName, name, dependencies)
            }
            else -> throw JsonParseException("Unknown GradleSwiftExportModuleType")
        }
    }

    override fun serialize(src: GradleSwiftExportModule?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        if (context == null) throw JsonParseException("Context is required for serializing files")
        if (src == null) {
            return JsonNull.INSTANCE
        }

        val jsonObject = JsonObject()

        when (src) {
            is GradleSwiftExportModule.BridgesToKotlin -> {
                jsonObject.add(FILES_FIELD, context.serialize(src.files))
                jsonObject.addProperty(BRIDGE_NAME_FIELD, src.bridgeName)
            }
            is GradleSwiftExportModule.SwiftOnly -> {
                jsonObject.add(SWIFT_API_FIELD, context.serialize(src.swiftApi))
            }
        }

        jsonObject.addProperty(NAME_FIELD, src.name)
        jsonObject.add(TYPE_FIELD, context.serialize(src.type))
        jsonObject.add(DEPENDENCIES_FIELD, context.serialize(src.dependencies))

        return jsonObject
    }
}

