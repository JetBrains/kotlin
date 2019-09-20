/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

/** Scopes for limiting what KmVisitors read. */
object Scope {

    /** Scopes for [KmDeclarationContainer]. */
    open class DeclarationContainer internal constructor(
        @JvmField val functions: Boolean,
        @JvmField val properties: Boolean,
        @JvmField val typeAliases: Boolean
    ) {
        companion object {
            val ALL = DeclarationContainer(
                functions = true,
                properties = true,
                typeAliases = true
            )
        }
    }

    /** Scopes for [KmClass]. */
    class Class(
        @JvmField val typeParameters: Boolean,
        @JvmField val constructors: Boolean,
        @JvmField val superTypes: Boolean,
        @JvmField val companionObject: Boolean,
        @JvmField val nestedClasses: Boolean,
        @JvmField val enumEntries: Boolean,
        @JvmField val sealedSubClasses: Boolean,
        @JvmField val extensions: Boolean,
        functions: Boolean,
        properties: Boolean,
        typeAliases: Boolean
    ) : DeclarationContainer(functions, properties, typeAliases) {
        companion object {
            val ALL = Class(
                typeParameters = true,
                constructors = true,
                superTypes = true,
                companionObject = true,
                nestedClasses = true,
                enumEntries = true,
                sealedSubClasses = true,
                extensions = true,
                functions = true,
                properties = true,
                typeAliases = true
            )
        }
    }
}
