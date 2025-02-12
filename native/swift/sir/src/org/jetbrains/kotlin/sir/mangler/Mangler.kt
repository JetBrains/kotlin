/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.mangler

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.SirSwiftModule

// See https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst for details

/**
 * Identifier in Swift.
 */
@JvmInline
private value class Identifier(val name: String)

private val SirNamed.identifier: Identifier
    get() = Identifier(name)

// TODO(KT-71023): support unicode in identifiers
private val MANGLEABLE_IDENTIFIER_REGEX = "[_a-zA-Z][_\$a-zA-Z0-9]*".toRegex()

/**
 * `name-length name`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#identifiers).
 *
 * This never produces substitutions. Only supports simple identifiers (matching `[_a-zA-Z][_$a-zA-Z0-9]*`).
 */
private val Identifier.mangledNameOrNull: String?
    get() {
        if (!name.matches(MANGLEABLE_IDENTIFIER_REGEX))
            return null
        return "${name.length}$name"
    }

/**
 * `identifier`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#declaration-contexts)
 */
public val SirModule.mangledNameOrNull: String?
    get() = when (this) {
        is SirSwiftModule -> "s"
        else -> identifier.mangledNameOrNull
    }

/**
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `context`
 */
public val SirDeclarationParent.mangledNameOrNull: String?
    get() = when (this) {
        is SirModule -> mangledNameOrNull
        is SirClass -> mangledNameOrNull
        is SirStruct -> mangledNameOrNull
        is SirEnum -> mangledNameOrNull
        is SirExtension -> mangledNameOrNull
        is SirVariable -> TODO()
        is SirProtocol -> mangledNameOrNull
    }

/**
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#entities) for `entity`
 */
public val SirNamedDeclaration.mangledNameOrNull: String?
    get() = when (this) {
        is SirClass -> mangledNameOrNull
        is SirEnum -> mangledNameOrNull
        is SirStruct -> mangledNameOrNull
        is SirTypealias -> TODO()
        is SirProtocol -> mangledNameOrNull
    }

/**
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types)
 */
public val SirType.mangledNameOrNull: String?
    get() = when (this) {
        is SirNominalType -> typeDeclaration.mangledNameOrNull
        is SirExistentialType -> null
        is SirErrorType -> null
        is SirUnsupportedType -> null
        is SirFunctionalType -> null
        is SirGenericType -> null
    }

/**
 * `entity module 'E'`
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `extension mangling`
 */
public val SirExtension.mangledNameOrNull: String?
    get() {
        require(parent is SirModule)
        return "${extendedType.mangledNameOrNull ?: return null}${parent.mangledNameOrNull ?: return null}E"
    }

/**
 * `context decl-name 'C'`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `nominal class type`
 *
 * Assumes that `decl-name` is always just an `identifier`.
 */
public val SirClass.mangledNameOrNull: String?
    get() {
        return "${parent.mangledNameOrNull ?: return null}${identifier.mangledNameOrNull ?: return null}C"
    }

/**
 * `context decl-name 'V'`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `nominal struct type`
 *
 * Assumes that `decl-name` is always just an `identifier`.
 */
public val SirStruct.mangledNameOrNull: String?
    get() {
        return "${parent.mangledNameOrNull ?: return null}${identifier.mangledNameOrNull ?: return null}V"
    }

/**
 * `context decl-name 'O'`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `nominal enum type`
 *
 * Assumes that `decl-name` is always just an `identifier`.
 */
public val SirEnum.mangledNameOrNull: String?
    get() {
        return "${parent.mangledNameOrNull ?: return null}${identifier.mangledNameOrNull ?: return null}O"
    }

/**
 * `protocol 'P'`
 *
 * See [spec](https://github.com/swiftlang/swift/blob/main/docs/ABI/Mangling.rst#types) for `nominal protocol type`
 *
 * Assumes that `decl-name` is always just an `identifier`.
 *
 * Note: Although the linked document has not been updated, SE-404/Swift5.10 nested protocols follow the same scheme as nested classes
 *
 */
public val SirProtocol.mangledNameOrNull: String?
    get() {
        return "${parent.mangledNameOrNull ?: return null}${identifier.mangledNameOrNull ?: return null}P"
    }