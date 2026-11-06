/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.backend.konan.ir.isAbstract
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.objcinterop.isObjCClass
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassHierarchy

internal val IrClass.needsInterfaceLookupTable: Boolean
    get() = !isInterface && !isAbstract() && !isObjCClass()


internal fun IrClass.buildClassHierarchyRecord(context: NativeBackendContext) = SerializedClassHierarchy(
        className = crossModuleName(),
        isInterface = isInterface,
        superClassName = getSuperClassNotAny()?.crossModuleName(),
        interfaceNames = superTypes.mapNotNull { it.classOrNull?.owner }
                .filter { it.isInterface }
                .map { it.crossModuleName() },
        interfaceVTableSize = if (isInterface) context.getLayoutBuilder(this).interfaceVTableEntries.size else 0,
        hasInterfaceLookupTable = needsInterfaceLookupTable,
)

/** The hierarchy piece contributed by [this] element, including the classes declared inside function bodies. */
internal fun IrElement.buildClassHierarchyRecords(context: NativeBackendContext): List<SerializedClassHierarchy> {
    val records = mutableMapOf<IrClass, SerializedClassHierarchy>()

    // A supertype of a recorded class may have no record of its own: e.g. an ObjC class.
    fun recordWithSupers(irClass: IrClass) {
        if (irClass in records) return
        // A class of a cached library is contributed by the slice of that cache instead, which stays the
        // authority on it: recording it from the deserialized IR here would shadow the slice, and mask
        // the diagnostic on a cache built without the hierarchy records.
        val library = irClass.getPackageFragment().module.kotlinLibrary
        if (library != null && context.config.cachedLibraries.isLibraryCached(library)) return
        irClass.superClasses.map { it.owner }.forEach(::recordWithSupers)
        records[irClass] = irClass.buildClassHierarchyRecord(context)
    }
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            recordWithSupers(declaration)
            super.visitClass(declaration)
        }
    })
    return records.values.toList()
}
