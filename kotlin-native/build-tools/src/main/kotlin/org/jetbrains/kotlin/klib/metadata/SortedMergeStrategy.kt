/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.klib.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.KlibModuleFragmentReadStrategy
import kotlinx.metadata.klib.fqName

/**
 * Output klib declarations in predictable sorted order.
 */
internal class SortedMergeStrategy : KlibModuleFragmentReadStrategy {

    override fun processModuleParts(parts: List<KmModuleFragment>): List<KmModuleFragment> =
            parts.fold(KmModuleFragment(), ::joinFragments).let(::listOf)
}

/**
 * We need a stable order for overloaded functions.
 */
internal fun KmFunction.mangle(): String {
    val typeParameters = typeParameters.joinToString(prefix = "<", postfix = ">", transform = KmTypeParameter::name)
    val valueParameters = valueParameters.joinToString(prefix = "(", postfix = ")", transform = KmValueParameter::name)
    val receiver = receiverParameterType?.classifier
    return "$receiver.${name}.$typeParameters.$valueParameters"
}

internal fun KmProperty.mangle(): String {
    val receiver = receiverParameterType?.classifier
    return "$receiver.$name"
}

private fun joinAndSortPackages(pkg1: KmPackage, pkg2: KmPackage) = KmPackage().apply {
    functions += (pkg1.functions + pkg2.functions).sortedBy(KmFunction::mangle)
    properties += (pkg1.properties + pkg2.properties).sortedBy(KmProperty::name)
    typeAliases += (pkg1.typeAliases + pkg2.typeAliases).sortedBy(KmTypeAlias::name)
}

/**
 * Merges two fragments of a single module into one.
 */
internal fun joinFragments(fragment1: KmModuleFragment, fragment2: KmModuleFragment) = KmModuleFragment().apply {
    assert(fragment1.fqName == fragment2.fqName)
    pkg = when {
        fragment1.pkg != null && fragment2.pkg != null -> joinAndSortPackages(fragment1.pkg!!, fragment2.pkg!!)
        fragment1.pkg != null -> joinAndSortPackages(fragment1.pkg!!, KmPackage())
        fragment2.pkg != null -> joinAndSortPackages(KmPackage(), fragment2.pkg!!)
        else -> null
    }
    fqName = fragment1.fqName
    classes += fragment1.classes.sortedBy(KmClass::name) + fragment2.classes.sortedBy(KmClass::name)
}