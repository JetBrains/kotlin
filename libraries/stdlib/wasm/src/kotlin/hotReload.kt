/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt.
 */

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.reftypes.anyref
import kotlin.wasm.internal.reftypes.funcref

/**
 * The hot-reload image's fdefinition table (-Xwasm-hot-reload): binding
 * name -> the function's current fdefinition cell. The cell itself is a
 * wasm GC struct the COMPILER emits (it holds a raw funcref, below
 * Kotlin's type level); here it is an opaque stored value, the same
 * discipline as [WasmModuleDescriptor]'s associated object getter.
 *
 * The table lives in the stdlib so the RESIDENT base instance owns the
 * image's state: every hot module's init resolves its bindings against
 * it through the ordinary cross-module import channel (BOTH wasm
 * targets — the base is one architecture), and a redefinition is one
 * write into the shared cell — the newest definition in the image is
 * live.
 *
 * TOP-LEVEL on purpose (the registerModuleDescriptor shape, not an
 * object): a member would take the singleton as a wasm receiver — and
 * the caller's OWN module-local singleton at that, so a delta would
 * write its own empty table instead of the base's. These functions ARE
 * cell-routed like everything else — GENESIS makes that safe: every
 * cell global initializes to its own impl, so the load path
 * self-hosts through the entire init.
 */
private class HotFdefnEntry(val cell: anyref)

private val hotFdefns = HashMap<String, HotFdefnEntry>()

/** Register (or overwrite) the cell a binding name resolves to. */
@UsedFromCompilerGeneratedCode
internal fun hotFdefnsRegister(name: String, cell: anyref) {
    hotFdefns[name] = HotFdefnEntry(cell)
}

/**
 * LOAD IS APPLY (the primitives era moved this here from emitted IR):
 * resolve the binding's cell against the image's table — mint and
 * register on first definition, reuse the shared cell on redefinition —
 * install THIS module's impl, return the cell. Built on the fdefn
 * primitives below, so the CELL ITSELF stays the raw one-field struct
 * the entries read with a single struct.get (the hot path is
 * untouched; only the logic around it became Kotlin).
 */
@UsedFromCompilerGeneratedCode
internal fun hotFdefnsResolve(name: String, impl: funcref): anyref {
    val entry = hotFdefns[name]
    val cell: anyref = if (entry != null) {
        entry.cell
    } else {
        val fresh = wasmFdefnNew(impl)
        hotFdefnsRegister(name, fresh)
        fresh
    }
    wasmFdefnSetFn(cell, impl)
    return cell
}

/**
 * The image's CLASS REGISTRY ("find-class"): each CONCRETE class's
 * load registers ONE LAYOUT — an ordinary Kotlin object holding the
 * shape's slot descriptors and the reader/writer/allocate funcrefs
 * the DEFINING module passed, PLUS the class's dispatch half (the
 * current vtable and itable the stepper installs) — into the
 * class-id -> CURRENT-layout map (load is apply: the newest
 * definition in the image is live).
 *
 * THE LAYOUT OBJECT: every payload carries a `$layout` pointer to the
 * layout object it was shaped by — one struct.get away; layout
 * equality is POINTER identity (SBCL's EQ). Migration runs only when
 * the instance's layout is not the class's current one — which (the
 * engine canonicalizes types by content) only happens when the shapes
 * genuinely differ.
 *
 * The remaining hashes only NAME things at compile time: the payload
 * TYPE key (a collision is a loud redefinition/link error) and the
 * slot tags (a type-tag collision would copy a wrong-typed value and
 * trap the writer's cast — never a silent lie). A compile consumes no
 * previous state; INSTANCES pin their defining modules through the
 * layout pointer.
 */
internal class HotSlot(val nameTag: Long, val typeTag: Long, val reader: funcref, val writer: funcref)

internal class HotLayout(
    /** the class's CURRENT vtable, itable and RTTI — the dispatch half
     *  of the class's definition ([hotVtableUpdate] installs them; the
     *  RTTI rides along so a class GAINING an interface flips `is` and
     *  interface dispatch on stale instances after one migration) */
    val vtable: anyref,
    val itable: anyref?,
    val rtti: anyref,
    val allocate: funcref,
    val slots: ArrayList<HotSlot>,
)

private val hotLayoutsCurrent = HashMap<Long, HotLayout>()

/** Begin (or overwrite — load is apply) the class's current layout and
 *  return it: the defining module's init stores the returned object
 *  into its per-class layout global, so this module's allocations
 *  stamp it into every fresh payload's `$layout`. */
@UsedFromCompilerGeneratedCode
internal fun hotLayoutsRegister(classId: Long, vtable: anyref, itable: anyref?, rtti: anyref, allocate: funcref): HotLayout {
    val layout = HotLayout(vtable, itable, rtti, allocate, ArrayList())
    hotLayoutsCurrent[classId] = layout
    return layout
}

/** Append one slot's descriptors and accessor closures. */
@UsedFromCompilerGeneratedCode
internal fun hotLayoutsAddSlot(classId: Long, nameTag: Long, typeTag: Long, reader: funcref, writer: funcref) {
    hotLayoutsCurrent[classId]!!.slots.add(HotSlot(nameTag, typeTag, reader, writer))
}

/** The class's CURRENT layout, for a module that only INSTANTIATES a
 *  foreign class (a stdlib value-class box, KClassImpl) and so has no
 *  layout global to read: its allocations stamp the returned object.
 *  Null for classes without layouts — never migrated. */
@UsedFromCompilerGeneratedCode
internal fun hotLayoutsCurrentOf(classId: Long): HotLayout? = hotLayoutsCurrent[classId]

/** The dispatch stepper — SBCL's wrapper update for METHODS: install
 *  the class's CURRENT vtable, itable and RTTI into the cell, so a method
 *  added by a newer generation is dispatchable on instances born
 *  before it existed (a grown vtable is a SUBTYPE — old callers'
 *  retry casts still pass), and return the vtable. A class the image
 *  doesn't know (machinery origins, layout-less) keeps its current
 *  vtable — the caller's own cast is the gate. */
@UsedFromCompilerGeneratedCode
internal fun hotVtableUpdate(cell: Any?): anyref {
    val layout = hotLayoutsCurrent[wasmGetRttiKlassId(cell)]
        ?: return wasmGetCellVtable(cell)
    return wasmInstallDispatch(cell, layout.vtable, layout.itable, layout.rtti)
}

/**
 * A function-typed slot's passage across the erased channel: funcrefs
 * are not anyref subtypes (wasm's func and any hierarchies are
 * disjoint), so the value rides INSIDE this ordinary object — the
 * reader boxes it (the raw cell path, like value-class boxing), the
 * writer unboxes it, and migration copies the box like any other
 * slot value.
 */
internal class HotFnBox(val fn: funcref)

/**
 * THE generic migrate — SBCL's update-instance-for-redefined-class, in
 * plain Kotlin: allocate a fresh payload of the CURRENT layout's shape
 * (type defaults baked), keep the live identity hash, copy every
 * same-named same-typed slot through the two layouts' accessor
 * closures, and swap the cell's payload IN PLACE — every holder
 * (globals, arrays, the host) sees the migrated instance; identity
 * never changes. The fresh payload's `$layout` is the current layout
 * (the allocate closure stamps its module's layout global), so the
 * instance is current from the swap on.
 */
private fun hotLayoutsUpdateInstance(cell: Any?, classId: Long, toLayout: HotLayout): anyref {
    val p = wasmGetPayload(cell)
    // the instance's own layout — NEVER null: every allocation stamps
    // one (same-module from the layout global, foreign from the
    // current-layout map), so a null here is a bug and !! says so
    val from = wasmGetPayloadLayout(p)!!
    val fresh = wasmCallSlotAllocate(toLayout.allocate)
    wasmSetPayloadHash(fresh, wasmGetPayloadHash(p))
    for (i in 0 until toLayout.slots.size) {
        val target = toLayout.slots[i]
        for (j in 0 until from.slots.size) {
            val source = from.slots[j]
            if (source.nameTag == target.nameTag && source.typeTag == target.typeTag) {
                wasmCallSlotWriter(target.writer, fresh, wasmCallSlotReader(source.reader, p))
                break
            }
        }
    }
    wasmSetPayload(cell, fresh)
    return fresh
}

/**
 * The OLD-code arm of the field-access lowering — SBCL's slot-value:
 * the class's CURRENT layout (keyed by the concrete class the
 * instance's RTTI names — the same content hash the layouts map keys
 * on, so inherited-field access through a supertype stays sound); if
 * the instance's own layout is not it (pointer identity), migrate in
 * place, then read the slot BY NAME+TYPE through the current layout.
 * A name that is gone (or whose type changed) throws — the honest
 * unbound-slot, and an in-language catchable one.
 */
@UsedFromCompilerGeneratedCode
internal fun hotLayoutsSlotValue(cell: Any?, nameTag: Long, typeTag: Long): anyref {
    val classId = wasmGetRttiKlassId(cell)
    val layout = hotLayoutsCurrent[classId]
        ?: error("hot reload: no current layout for class $classId")
    var p = wasmGetPayload(cell)
    if (wasmGetPayloadLayout(p) !== layout) {
        p = hotLayoutsUpdateInstance(cell, classId, layout)
    }
    for (slot in layout.slots) {
        if (slot.nameTag == nameTag && slot.typeTag == typeTag) return wasmCallSlotReader(slot.reader, p)
    }
    error("hot reload: unbound slot $nameTag:$typeTag of class $classId")
}

/** (setf slot-value) — the write mirror of [hotLayoutsSlotValue]. */
@UsedFromCompilerGeneratedCode
internal fun hotLayoutsSetSlotValue(cell: Any?, nameTag: Long, typeTag: Long, value: anyref) {
    val classId = wasmGetRttiKlassId(cell)
    val layout = hotLayoutsCurrent[classId]
        ?: error("hot reload: no current layout for class $classId")
    val p = if (wasmGetPayloadLayout(wasmGetPayload(cell)) !== layout) {
        hotLayoutsUpdateInstance(cell, classId, layout)
    } else {
        wasmGetPayload(cell)
    }
    for (slot in layout.slots) {
        if (slot.nameTag == nameTag && slot.typeTag == typeTag) {
            wasmCallSlotWriter(slot.writer, p, value)
            return
        }
    }
    error("hot reload: unbound slot $nameTag:$typeTag of class $classId")
}

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmGetPayload(cell: Any?): anyref = implementedAsIntrinsic

/** The cell's CURRENT vtable (the stepper's unknown-class fallback). */
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmGetCellVtable(cell: Any?): anyref = implementedAsIntrinsic

/** Install the vtable, itable and RTTI into the cell's three mutable
 *  dispatch fields; returns the vtable. */
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmInstallDispatch(cell: Any?, vtable: anyref, itable: anyref?, rtti: anyref): anyref = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmSetPayload(cell: Any?, payload: anyref): Unit = implementedAsIntrinsic

/** The instance's own LAYOUT — the payload's `$layout` field: the
 *  layout object the payload was shaped by (pointer identity — SBCL's
 *  EQ check). */
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmGetPayloadLayout(payload: anyref): HotLayout? = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmGetPayloadHash(payload: anyref): Int = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmSetPayloadHash(payload: anyref, value: Int): Unit = implementedAsIntrinsic

/** The instance's CONCRETE class identity — the RTTI's content hash,
 *  the same id the layouts table keys on. */
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmGetRttiKlassId(cell: Any?): Long = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmCallSlotReader(reader: funcref, payload: anyref): anyref = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmCallSlotWriter(writer: funcref, payload: anyref, value: anyref): Unit = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmCallSlotAllocate(allocate: funcref): anyref = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmFdefnNew(fn: funcref): anyref = implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmFdefnSetFn(cell: anyref, fn: funcref): Unit = implementedAsIntrinsic
