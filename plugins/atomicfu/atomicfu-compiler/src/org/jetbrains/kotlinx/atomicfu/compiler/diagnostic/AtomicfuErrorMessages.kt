/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

object AtomicfuErrorMessages : BaseDiagnosticRendererFactory() {

    const val CONSTRAINTS_MESSAGE =
        "\nPlease make sure that you follow these constraints for using atomic properties:\n" +
                "   * To ensure that atomic properties are not accessed out of the current Kotlin module, it is necessary to declare atomic properties as private or internal.\n" +
                "     Alternatively, you can make the containing class private or internal.\n" +
                "     If you need to expose the atomic property value to the public, consider using a delegated property declared within the same scope:\n" +
                "       ```\n" +
                "       private val _a = atomic<T>(initial) \n" +
                "       public var a: T by _a \n" +
                "       ```\n" +
                "   * Directly invoke operations on atomic properties, like this:\n" +
                "       ```\n" +
                "       val top = atomic<Node?>(null)\n" +
                "       top.compareAndSet(null, Node(1)) // OK\n" +
                "       ```\n" +
                "   * Refrain from invoking atomic operations on local variables:\n" +
                "       ```\n" +
                "       val top = atomic<Node?>(null)\n" +
                "       val tmp = top\n" +
                "       tmp.compareAndSet(null, Node(1)) // DON'T DO THIS\n" +
                "       ```\n" +
                "   * Avoid leaking references to atomic values in other ways, such as returning them or passing them as parameters.\n" +
                "   * Be cautious with the complexity of data flow within parameters of atomic operations.\n" +
                "     For instance, instead of using intricate expression directly as an argument, e.g.:\n" +
                "       ```\n" +
                "       top.compareAndSet(cur, <complex_expression>)\n" +
                "       ```\n" +
                "     create a separate variable to hold the complex expression's value and then perform the operation:\n" +
                "       ```\n" +
                "       val newValue = <complex_expression>\n" +
                "       top.compareAndSet(cur, newValue) \n" +
                "       ```\n" +
                "\n"

    private const val PUBLIC_ATOMICS_ARE_FORBIDDEN_MESSAGE =
        "\nTo prevent atomic properties from being referenced outside the current Kotlin module, they should be declared as either private or internal. " +
                "Note, that `@kotlin.PublishedApi` annotation, when applied to a class or a member with internal visibility, makes it effectively public.\n" +
                "Please consider setting the visibility of the property `''{0}''` to private or internal or limit the scope of the containing class. \n" +
                "Alternatively, if you need to expose the atomic property value to the public, you can use a delegated property declared within the same scope, e.g:\n" +
                "```\n" +
                "private val _a = atomic<T>(initial) \n" +
                "public val a: T by _a \n" +
                "```\n"

    private const val ATOMIC_PROEPRTIES_SHOULD_BE_VAL_MESSAGE = "Please consider declaring `''{0}''` as a private val or internal val.\n" +
            "If you need to declare a variable with accessors delegated to the atomic property value, you can use a delegated property declared within the same scope, e.g:\n" +
            "```\n" +
            "private val _a = atomic<T>(initial) \n" +
            "public var a: T by _a \n" +
            "```\n"

    override val MAP: KtDiagnosticFactoryToRendererMap = KtDiagnosticFactoryToRendererMap("Atomicfu Plugin").also { map ->
        map.put(
            AtomicfuErrors.PUBLIC_ATOMICS_ARE_FORBIDDEN, PUBLIC_ATOMICS_ARE_FORBIDDEN_MESSAGE, Renderers.TO_STRING
        )
        map.put(
            AtomicfuErrors.PUBLISHED_API_ATOMICS_ARE_FORBIDDEN, PUBLIC_ATOMICS_ARE_FORBIDDEN_MESSAGE, Renderers.TO_STRING
        )
        map.put(
            AtomicfuErrors.ATOMIC_PROPERTIES_SHOULD_BE_VAL, ATOMIC_PROEPRTIES_SHOULD_BE_VAL_MESSAGE, Renderers.TO_STRING
        )
    }

}
