/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.export

import org.jetbrains.kotlin.ir.backend.js.*
import org.junit.Test
import org.junit.Assert.assertEquals


fun namespace(name: String, vararg declarations: ExportedDeclaration) =
    ExportedNamespace(name, declarations.toList())

fun module(vararg declarations: ExportedDeclaration) =
    ExportedModule(declarations.toList())

class ExportStructureToTypeScriptTest {

    private fun testDeclaration(expected: String, actual: ExportedDeclaration) {
        assertEquals(expected + "\n", actual.toTypeScript(""))
    }

    @Test
    fun functions() {
        val function = ExportedFunction(
            name = "funcExample",
            returnType = ExportedType.Primitive.Number,
            parameters = listOf(
                ExportedParameter("x", ExportedType.Primitive.String),
                ExportedParameter("y", ExportedType.Primitive.Number)
            )
        )

        testDeclaration("function funcExample(x: string, y: number): number", function)

        val function2 = ExportedFunction(
            name = "funcExample2",
            returnType = ExportedType.Primitive.Unit,
            parameters = emptyList()
        )

        testDeclaration("function funcExample2(): void", function2)
    }

    @Test
    fun property() {
        val property = ExportedProperty(
            name = "propExample",
            type = ExportedType.Array(ExportedType.Primitive.Number),
            mutable = true
        )

        testDeclaration("let propExample: Array<number>", property)

        val property2 = ExportedProperty(
            name = "propExample",
            type = ExportedType.Primitive.String,
            mutable = false
        )

        testDeclaration("const propExample: string", property2)
    }

    @Test
    fun klass() {

        val prop = ExportedProperty(
            name = "propExample",
            type = ExportedType.Array(ExportedType.Primitive.Number),
            mutable = true,
            isMember = true
        )

        val function = ExportedFunction(
            name = "funcExample",
            returnType = ExportedType.Primitive.Number,
            parameters = listOf(
                ExportedParameter("x", ExportedType.Primitive.String),
                ExportedParameter("y", ExportedType.Primitive.Number)
            ),
            isMember = true
        )

        val klass = ExportedClass(
            name = "classExample",
            superClass = null,
            typeParameters = emptyList(),
            members = listOf(prop, function)
        )


        testDeclaration(
            """class classExample {
  propExample: Array<number>

  funcExample(x: string, y: number): number
}""", klass)
    }


    @Test
    fun namespace() {
        val function = ExportedFunction(
            name = "funcExample",
            returnType = ExportedType.Primitive.Number,
            parameters = listOf(
                ExportedParameter("x", ExportedType.Primitive.String)
            )
        )

        val mutProperty = ExportedProperty(
            name = "mutablePropExample",
            type = ExportedType.Primitive.Boolean,
            mutable = true
        )

        val immutProperty = ExportedProperty(
            name = "immutablePropExample",
            type = ExportedType.Primitive.Any,
            mutable = false
        )

        val module = module(
            namespace(
                "top",
                namespace("nested1", function, mutProperty),
                namespace("nested2", namespace("nested2_nested", immutProperty))
            )
        )

        val ts = module.toTypeScript()
        println(ts)

        assertEquals(
            """
            |declare namespace top {
            |  namespace nested1 {
            |    function funcExample(x: string): number
            |
            |    let mutablePropExample: boolean
            |  }
            |
            |  namespace nested2 {
            |    namespace nested2_nested {
            |      const immutablePropExample: any
            |    }
            |  }
            |}
            |""".trimMargin("|"),
            ts
        )
    }
}