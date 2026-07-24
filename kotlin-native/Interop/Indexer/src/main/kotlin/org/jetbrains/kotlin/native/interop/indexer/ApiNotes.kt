/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/**
 * `SwiftName` refinements extracted from Clang API notes (`*.apinotes`) files.
 *
 * The type hierarchy mirrors `clang/APINotes/Types.h` so that the semantics stay recognizable, but only the
 * `SwiftName` field is kept, for classes, protocols, and their methods and properties. Every other refinement
 * (nullability, availability, bridging, etc.) is intentionally dropped.
 *
 * The versioned `SwiftVersions:` section is also ignored on purpose. Those entries hold names for *older* Swift
 * language modes; the top-level `SwiftName` is the current one, which is what Clang selects for any modern Swift
 * version (see `APINotesReader::VersionedInfo`).
 *
 * @see load for the (modular-only) discovery rules.
 */
class ApiNotes private constructor(
        private val classes: Map<String, Context>,
        private val protocols: Map<String, Context>,
) {
    fun objCClass(name: String): Context? = classes[name]

    fun protocol(name: String): Context? = protocols[name]

    open class CommonEntityInfo(val swiftName: String?)

    class Method(swiftName: String?) : CommonEntityInfo(swiftName)

    class Property(swiftName: String?) : CommonEntityInfo(swiftName)

    /** An Objective-C class or protocol context, holding the notes for its members. */
    class Context internal constructor(
            swiftName: String?,
            private val instanceMethods: Map<String, Method>,
            private val classMethods: Map<String, Method>,
            private val instanceProperties: Map<String, Property>,
            private val classProperties: Map<String, Property>,
    ) : CommonEntityInfo(swiftName) {
        fun method(selector: String, isInstance: Boolean): Method? =
                (if (isInstance) instanceMethods else classMethods)[selector]

        fun property(name: String, isInstance: Boolean): Property? =
                (if (isInstance) instanceProperties else classProperties)[name]
    }

    companion object {
        /**
         * Discovers and parses the API notes applicable to [library], following Clang's discovery rules for
         * modular interop: for each imported module, an `<ModuleName>.apinotes` file is looked up next to the
         * module's headers (e.g. `Foo.framework/.../Headers/Foo.apinotes`).
         *
         * Header-based (non-modular) interop is not covered yet; such libraries produce null.
         */
        fun load(library: NativeLibrary): ApiNotes? {
            val headerFilter = library.headerFilter
            if (headerFilter !is NativeLibraryHeaderFilter.Predefined || headerFilter.modules.isEmpty()) {
                return null
            }

            // Clang looks for `<ModuleName>.apinotes` in the module's headers directory. For every module header we
            // already resolved, its parent directory is that headers directory (this also covers versioned frameworks,
            // whose headers live in `Foo.framework/Versions/X/Headers`).
            val headerDirs = headerFilter.headers.mapNotNullTo(linkedSetOf()) { File(it).parentFile }

            val files = linkedSetOf<File>()
            for (module in headerFilter.modules) {
                for (dir in headerDirs) {
                    val file = File(dir, "$module.apinotes")
                    if (file.isFile) files += file
                }
            }
            if (files.isEmpty()) return null

            val classes = mutableMapOf<String, Context>()
            val protocols = mutableMapOf<String, Context>()
            for (file in files) {
                val module = runCatching { mapper.readValue<ApiNotesModule>(file) }.getOrNull() ?: continue
                module.classes.forEach { classes.putContext(it) }
                module.protocols.forEach { protocols.putContext(it) }
            }

            return ApiNotes(classes, protocols)
        }

        private val mapper = JsonMapper.builder(YAMLFactory())
                .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()
                .registerKotlinModule()

        private fun MutableMap<String, Context>.putContext(raw: ApiNotesContext) {
            val instanceMethods = mutableMapOf<String, Method>()
            val classMethods = mutableMapOf<String, Method>()
            for (method in raw.methods) {
                val swiftName = method.swiftName.blankToNull() ?: continue
                val target = if (method.methodKind == EntityKind.Instance) instanceMethods else classMethods
                target[method.selector] = Method(swiftName)
            }

            val instanceProperties = mutableMapOf<String, Property>()
            val classProperties = mutableMapOf<String, Property>()
            for (property in raw.properties) {
                val swiftName = property.swiftName.blankToNull() ?: continue
                // A property without a PropertyKind applies to both the instance and the class property (matching Clang).
                if (property.propertyKind != EntityKind.Class) instanceProperties[property.name] = Property(swiftName)
                if (property.propertyKind != EntityKind.Instance) classProperties[property.name] = Property(swiftName)
            }

            val swiftName = raw.swiftName.blankToNull()
            if (swiftName == null && instanceMethods.isEmpty() && classMethods.isEmpty() &&
                    instanceProperties.isEmpty() && classProperties.isEmpty()
            ) return
            this[raw.name] = Context(swiftName, instanceMethods, classMethods, instanceProperties, classProperties)
        }

        private fun String?.blankToNull(): String? = this?.takeIf { it.isNotBlank() }
    }
}

// The data classes below map only the subset of the API notes schema we consume. Unknown keys (SwiftBridge,
// Nullability, Availability, SwiftVersions, Functions, Globals, Enumerators, Tags, Typedefs, ...) are ignored
// via the mapper's disabled FAIL_ON_UNKNOWN_PROPERTIES.

/** `MethodKind`/`PropertyKind` in API notes. `PropertyKind` may be absent, meaning "both". */
private enum class EntityKind { Class, Instance }

private data class ApiNotesModule(
        val classes: List<ApiNotesContext> = emptyList(),
        val protocols: List<ApiNotesContext> = emptyList(),
)

private data class ApiNotesContext(
        val name: String,
        val swiftName: String? = null,
        val methods: List<ApiNotesMethod> = emptyList(),
        val properties: List<ApiNotesProperty> = emptyList(),
)

private data class ApiNotesMethod(
        val selector: String,
        val methodKind: EntityKind,
        val swiftName: String? = null,
)

private data class ApiNotesProperty(
        val name: String,
        val propertyKind: EntityKind? = null,
        val swiftName: String? = null,
)
