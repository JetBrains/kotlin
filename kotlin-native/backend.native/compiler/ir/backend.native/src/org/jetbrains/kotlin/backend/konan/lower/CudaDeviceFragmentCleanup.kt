/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.Name

/**
 * Prunes IR declarations from `@CudaCompile` files that would either break or clutter the
 * device-fragment PTX output. Three concerns, handled together because they all walk the
 * same file/class structure:
 *
 *  1. **`$instance` / `$companion` synthetic accessor properties (correctness).**
 *     `ObjectClassLowering.processObjectClass` adds these to every non-`Unit` object and to
 *     every class with a companion. Their getter body uses `createUninitializedInstance` /
 *     `initInstance` plus the implicit `UpdateReturnRef` write-barrier on the epilogue —
 *     runtime symbols that don't exist in the CUDA device runtime module. Without this
 *     scrub, device codegen fails at `getUpdateReturnRefFunction` with "Runtime function
 *     `UpdateReturnRef` not found in this runtime module".
 *
 *     Safe to drop because `DropTrivialObjectInstancesLowering` already replaced every
 *     `IrGetObjectValue` for a stateless object with a `Unit` composite, and any in-kernel
 *     `Foo.bar()` call had its `Foo.INSTANCE` receiver elided the same way (the host-side
 *     launchpad lowering fabricates a null receiver word when launching the kernel).
 *
 *  2. **`const val` properties (cleanliness).**
 *     Kotlin's frontend const-folds every read of a `const val` to an [IrConst] at the use
 *     site, so the property declaration itself is dead. The synthesized public getter,
 *     however, has external linkage — `globaldce` in the device IR cleanup pipeline can't
 *     drop it, and it survives as a noisy stub in the emitted PTX (e.g.
 *     `kfun_demo__get_BlockSize_____kotlin_Int { st.param.b32 [func_retval0], 16; ret; }`).
 *     Removing the property removes its getter, and DCE then prunes the rest.
 *
 *  3. **Object constructors, their lowered constructor functions, and anonymous
 *     initializers (correctness).**
 *     K/N emits an instance backing field for an object-member `const val` and moves its
 *     initializer into the object's primary constructor as `<this>.#FIELD = <literal>`.
 *     The constructor also returns `theUnitInstance()` at its epilogue — a host-runtime
 *     symbol. Both blow up device codegen: the field write references the [IrField] we
 *     deleted in (2), and the `theUnitInstance` return needs a runtime symbol that
 *     doesn't exist on the device.
 *
 *     By the time this phase runs, `ConstructorsLowering` has already replaced each
 *     `IrConstructor` in `clazz.declarations` with `[IrConstructor, IrSimpleFunction]` —
 *     the simple-function carries the moved body and is the one codegen actually emits.
 *     Both need to go; the simple-function is identified by a non-null `originalConstructor`
 *     attribute. The body-less `IrConstructor` is kept in `declarations` to satisfy IR
 *     invariants for things like `inline class` underlying-type lookup, but removing it
 *     from an `object` declaration is safe.
 *
 *     Safe to drop on the device side because the only call site that constructs an
 *     object instance is the `$instance` accessor body, and we removed that in (1). The
 *     trivial-object lowering already elided every `IrGetObjectValue` for stateless
 *     objects, so no in-kernel code path needs an actual `Foo` value.
 *
 * Runs late, after the full file-lowering pipeline (so `objectClassesPhase` has already
 * created the accessor properties to scrub) and before `CreateLLVMDeclarationsPhase` (so
 * codegen never sees the removed declarations).
 *
 * No-op outside the device fragment: the host fragment never lowers `@CudaCompile` files
 * and needs all of these declarations verbatim.
 */
internal val CudaDeviceFragmentCleanupPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "CudaDeviceFragmentCleanup",
) { _, module ->
    val syntheticAccessorNames = setOf(
            "instance".synthesizedName,
            "companion".synthesizedName,
    )
    module.files
            .filter { it.hasAnnotation(KonanFqNames.cudaCompile) }
            .forEach { file ->
                // File-level: only const-val properties exist as removable noise; no
                // constructors/initializers live at the file top level.
                file.declarations.removeAll { it is IrProperty && it.isConst }
                for (member in file.declarations) {
                    if (member is IrClass) pruneClass(member, syntheticAccessorNames)
                }
            }
}

private fun pruneClass(clazz: IrClass, syntheticAccessorNames: Set<Name>) {
    val isObject = clazz.isObject
    clazz.declarations.removeAll { decl ->
        when (decl) {
            is IrProperty -> decl.name in syntheticAccessorNames || decl.isConst
            // Limit constructor/init removal to objects: regular classes inside a
            // `@CudaCompile` file (e.g. value classes used as kernel arg types) keep their
            // constructors so any user code that legitimately instantiates them still
            // codegens. For objects we drop both forms — the `IrConstructor` shell and the
            // lowered `IrSimpleFunction` that `ConstructorsLowering` moved the body into
            // (identifiable by a non-null `originalConstructor` attribute).
            is IrConstructor -> isObject
            is IrSimpleFunction -> isObject && decl.originalConstructor != null
            is IrAnonymousInitializer -> isObject
            else -> false
        }
    }
    for (member in clazz.declarations) {
        if (member is IrClass) pruneClass(member, syntheticAccessorNames)
    }
}
