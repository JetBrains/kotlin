/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

import java.lang.invoke.CallSite
import java.lang.invoke.ConstantCallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

public object DataCopyBootstrap {
    public const val MAX_COMPONENT_INDEX: Int = 100

    @OptIn(ExperimentalStdlibApi::class)
    @JvmStatic
    public fun bootstrap(
        lookup: MethodHandles.Lookup,
        name: String,
        type: MethodType,
        // these will eventually be given from the constant pool
        // https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.4
        klass: Class<*>,
        vararg givenComponents: Int,
    ): CallSite? {
        // requirements
        val kotlinKlass = klass.kotlin
        require(name == "copy") { "Only copy is supported" }
        val copyHandle = klass.declaredMethods.singleOrNull { it.name == "copy" }
        require(copyHandle != null) { "No copy method found" }

        // create list of component functions
        val components = mutableListOf<MethodHandle>()
        val reordering = mutableListOf<Int>(0) // always start with 'this'
        for (componentIndex in 1 .. MAX_COMPONENT_INDEX) {
            val componentNMethod = klass.getDeclaredMethod("component$componentIndex")
            // if this componentN does not exist, we're done
            if (componentNMethod == null) break
            when (val givenIndex = givenComponents.indexOf(componentIndex)) {
                -1 -> {
                    components += lookup.unreflect(componentNMethod)
                    reordering += 0
                }
                else -> {
                    val componentType = componentNMethod.returnType
                    components += MethodHandles.identity(componentType)
                    reordering += givenIndex + 1
                }
            }


        }

        // create the body of the MethodHandle
        val copyWithGetters = MethodHandles.filterArguments(lookup.unreflect(copyHandle), 1, *components.toTypedArray())
        val copyReordered = MethodHandles.permuteArguments(copyWithGetters, type, *reordering.toIntArray())
        return ConstantCallSite(copyReordered)
    }
}