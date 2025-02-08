/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

import kotlin.reflect.KClass

internal class KClassInterfaceImpl<T : Any> @WasmPrimitiveConstructor constructor(internal val typeData: TypeInfoData) : KClass<T> {
    override val simpleName: String get() = typeData.typeName
    override val qualifiedName: String
        get() = if (typeData.packageName.isEmpty()) typeData.typeName else "${typeData.packageName}.${typeData.typeName}"

    override fun isInstance(value: Any?): Boolean {
        if (value !is Any) return false
        return getInterfaceSlot(value, typeData.typeId) != -1
    }

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is KClassInterfaceImpl<*> && other.typeData.typeId == typeData.typeId)

    override fun hashCode(): Int = typeData.typeId.toInt()

    override fun toString(): String = "interface $qualifiedName"
}