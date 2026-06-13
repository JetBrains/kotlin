/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments

/** A predicate which checks whether the given declaration is entry point, and should be exposed in Objective-C. */
interface ObjCEntryPoints {
    fun shouldBeExposed(descriptor: CallableMemberDescriptor) = true

    companion object {
        val ALL: ObjCEntryPoints = object : ObjCEntryPoints {}

        fun create(descriptors: Set<CallableMemberDescriptor>): ObjCEntryPoints =
            object : ObjCEntryPoints {
                override fun shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean =
                    descriptors.contains(descriptor.original)
            }
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

fun computeDownwardClosure(
    entryPoints: ObjCEntryPoints,
    moduleDescriptors: List<ModuleDescriptor>
): Set<CallableMemberDescriptor> {
    val overriddenToOverride = mutableMapOf<CallableMemberDescriptor, MutableSet<CallableMemberDescriptor>>()
    val matched = mutableSetOf<CallableMemberDescriptor>()

    fun processCallable(descriptor: CallableMemberDescriptor) {
        descriptor.overriddenDescriptors.forEach { overridden ->
            overriddenToOverride.getOrPut(overridden.original, ::mutableSetOf).add(descriptor)
        }
        if (entryPoints.shouldBeExposed(descriptor)) {
            matched.add(descriptor.original)
        }
    }

    fun processClass(descriptor: ClassDescriptor) {
        descriptor.unsubstitutedMemberScope.getContributedDescriptors().forEach { descriptor ->
            when (descriptor) {
                is CallableMemberDescriptor -> processCallable(descriptor)
                is ClassDescriptor -> processClass(descriptor)
            }
        }
    }

    moduleDescriptors.forEach {
        it.getPackageFragments().forEach { fragment ->
            fragment.getMemberScope().getContributedDescriptors().forEach { descriptor ->
                when (descriptor) {
                    is CallableMemberDescriptor -> processCallable(descriptor)
                    is ClassDescriptor -> processClass(descriptor)
                }
            }
        }
    }

    val closure = mutableSetOf<CallableMemberDescriptor>()
    val todo = matched.toMutableList()

    while (todo.isNotEmpty()) {
        val current = todo.removeLast()
        if (closure.add(current)) {
            overriddenToOverride[current]?.forEach { override ->
                todo.add(override.original)
            }
        }
    }

    return closure
}
