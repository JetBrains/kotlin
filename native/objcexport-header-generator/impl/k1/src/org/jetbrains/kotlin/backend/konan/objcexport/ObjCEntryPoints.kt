/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/** A predicate which checks whether the given declaration is entry point, and should be exposed in Objective-C. */
interface ObjCEntryPoints {
    fun shouldBeExposed(descriptor: CallableMemberDescriptor) = true

    companion object {
        val ALL: ObjCEntryPoints = object : ObjCEntryPoints {}
    }
}

/**
 * Reads entry points from this file.
 */
fun File.readObjCEntryPoints(): ObjCEntryPoints =
    readObjCEntryPointList()
        .toSet()
        .let { entryPointSet ->
            object : ObjCEntryPoints {
                override fun shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean =
                    descriptor.objCEntryPointKindOrNull
                        ?.let { objcEntryPointKind -> shouldBeExposed(objcEntryPointKind, descriptor.fqNameSafe) }
                        ?: false

                private fun shouldBeExposed(kind: ObjCEntryPoint.Kind, fqName: FqName): Boolean =
                    entryPointSet.contains(ObjCEntryPoint(kind, fqName.toObjCExplicitPattern())) ||
                        entryPointSet.contains(ObjCEntryPoint(kind, fqName.toObjCWildcardPattern())) ||
                        kind.parentOrNull?.let { shouldBeExposed(it, fqName) }.let { it ?: false }

                /** A kind which matches this descriptor. */
                private val DeclarationDescriptor.objCEntryPointKindOrNull: ObjCEntryPoint.Kind?
                    get() = when (this) {
                        is FunctionDescriptor -> ObjCEntryPoint.Kind.FUNCTION
                        is PropertyDescriptor -> ObjCEntryPoint.Kind.PROPERTY
                        is CallableDescriptor -> ObjCEntryPoint.Kind.CALLABLE
                        else -> null
                    }

                /** Convert this fully-qualified name to a pattern path, containing all but last components. */
                private fun FqName.toObjCPatternPath(): List<String> =
                    pathSegments().map { it.asString() }

                /** Convert this fully-qualified name to an explicit pattern. */
                private fun FqName.toObjCExplicitPattern(): ObjCEntryPoint.Pattern =
                    ObjCEntryPoint.Pattern(
                        parent().toObjCPatternPath(),
                        ObjCEntryPoint.Pattern.Name.Explicit(shortName().asString())
                    )

                /** Convert this fully-qualified name to a wildcard pattern. */
                private fun FqName.toObjCWildcardPattern(): ObjCEntryPoint.Pattern =
                    ObjCEntryPoint.Pattern(
                        parent().toObjCPatternPath(),
                        ObjCEntryPoint.Pattern.Name.Wildcard
                    )
            }
        }
