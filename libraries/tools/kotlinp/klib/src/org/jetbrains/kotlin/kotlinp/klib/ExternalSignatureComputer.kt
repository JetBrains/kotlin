/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.klib

import kotlin.metadata.*
import kotlinx.metadata.klib.KlibEnumEntry
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.fqName
import kotlinx.metadata.klib.klibEnumEntries
import java.util.IdentityHashMap

/**
 * An auxiliary tool that allows computing and caching IR signatures for the metadata declarations rendered
 * by [KlibKotlinp]. Such signatures are not available directly in the metadata, but they can be computed by
 * descriptors (K1) or Lazy IR built over FIR (K2) and then properly mapped to the corresponding `Km*` entities.
 */
class ExternalSignatureComputer(module: KlibModuleMetadata, getExternallyComputedSignature: (DeclarationId) -> String?) {
    /**
     * Note on [IdentityHashMap]: It's presence here might look strange. But in fact this is an approach that allows memorizing
     * externally computed signatures and then looking up them using `Km*` entities as keys. I.e. no context needs to be maintained
     * while traversing the `Km*`-tree (like: what is the containing declaration of a particular [KmFunction]?).
     */
    private val signatures = IdentityHashMap<Any, String>()
    private val getterSignatures = IdentityHashMap<KmProperty, String>()
    private val setterSignatures = IdentityHashMap<KmProperty, String>()

    init {
        fun visitDeclarationContainer(containerNamePrefix: String, container: KmDeclarationContainer) {
            container.functions.forEach { function ->
                signatures[function] = getExternallyComputedSignature(function.functionId(containerNamePrefix))
            }
            container.properties.forEach { property ->
                val propertyId = property.propertyId(containerNamePrefix)
                signatures[property] = getExternallyComputedSignature(propertyId)
                getterSignatures[property] = getExternallyComputedSignature(property.getterId(propertyId))
                if (property.setter != null) {
                    setterSignatures[property] = getExternallyComputedSignature(property.setterId(propertyId, property.setterParameter))
                }
            }
            container.typeAliases.forEach { typeAlias ->
                signatures[typeAlias] = getExternallyComputedSignature(typeAlias.typeAliasId(containerNamePrefix))
            }
        }

        module.fragments.forEach { fragment ->
            fragment.classes.forEach { clazz ->
                signatures[clazz] = getExternallyComputedSignature(clazz.classId())
                val classNamePrefix = "${clazz.name}."
                clazz.constructors.forEach { constructor ->
                    signatures[constructor] = getExternallyComputedSignature(constructor.constructorId(classNamePrefix))
                }
                visitDeclarationContainer(classNamePrefix, clazz)
                clazz.klibEnumEntries.forEach { enumEntry ->
                    signatures[enumEntry] = getExternallyComputedSignature(clazz.enumEntryId(enumEntry))
                }
            }
            fragment.pkg?.let {
                val packageName = fragment.fqName?.replace('.', '/').orEmpty()
                visitDeclarationContainer(if (packageName.isNotEmpty()) "$packageName/" else "", it)
            }
        }
    }

    fun classSignature(clazz: KmClass): String? = signatures[clazz]
    fun enumEntrySignature(enumEntry: KlibEnumEntry): String? = signatures[enumEntry]
    fun constructorSignature(constructor: KmConstructor): String? = signatures[constructor]
    fun functionSignature(function: KmFunction): String? = signatures[function]
    fun propertySignature(property: KmProperty): String? = signatures[property]
    fun propertyGetterSignature(property: KmProperty): String? = getterSignatures[property]
    fun propertySetterSignature(property: KmProperty): String? = setterSignatures[property]
    fun typeAliasSignature(typeAlias: KmTypeAlias): String? = signatures[typeAlias]
}
