/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.ast

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.inline.util.fixForwardNameReferences
import org.jetbrains.kotlin.js.parser.parse
import org.junit.Test
import kotlin.test.assertEquals

class FixForwardReferencesTest {

    @Test
    fun simple() {
        """
            function foo() {
                var a = 1;
                return a;
            }
        """ parsesInto """
            function foo_0() {
                var a_0 = 1;
                return a_0;
            }
        """ transformsInto """
            function foo_0() {
              var a_0 = 1;
              return a_0;
            }
        """
    }

    @Test
    fun functionParameterClashesWithCatch() {
        """
            function foo(a) {
                try {} catch (a) {
                    a;
                }
                try {} catch (a) {
                    a;
                }
                return a;
            }
        """ parsesInto """
            function foo_0(a_0) {
                try {} catch (a_1) {
                    a_1;
                }
                try {} catch (a_2) {
                    a_2;
                }
                return a_0;
            }
        """ transformsInto """
            function foo_0(a_0) {
                try {} catch (a_1) {
                    a_1;
                }
                try {} catch (a_2) {
                    a_2;
                }
                return a_0;
            }
        """
    }

    @Test
    fun nestedCatchParameters() {
        """
            try {} catch (a) {
                a;
                try {
                    a;
                } catch (a) {
                    a;
                }
                a;
            }
        """ parsesInto """
            try {} catch (a_0) {
                a_0;
                try {
                    a_0;
                } catch (a_1) {
                    a_1;
                }
                a_0;
            }
        """ transformsInto """
            try {} catch (a_0) {
                a_0;
                try {
                    a_0;
                } catch (a_1) {
                    a_1;
                }
                a_0;
            }
        """
    }

    @Test
    fun labelsAndVarClash() {
        """
            function () {
                var a;
                a: while (a) {
                    break a;
                }
                a;
            }
        """ parsesInto """
            function () {
                var a_0;
                a_1: while (a_0) {
                    break a_1;
                }
                a_0;
            }
        """ transformsInto """
            function () {
                var a_0;
                a_1: while (a_0) {
                    break a_1;
                }
                a_0;
            }
        """
    }

    @Test fun catchParamAndVarClash() {
        """
            {
                var a;
                try {} catch (a) { a }
                a;
            }
        """ parsesInto """
            {
                var a_0;
                try {} catch (a_1) { a_1 }
                a_0;
            }
        """ transformsInto """
            {
                var a_0;
                try {} catch (a_1) { a_1 }
                a_0;
            }
        """
    }

    @Test fun catchParamAndVarClashInsideFunction() {
        """
            function () {
                var a;
                try {} catch (a) { a }
                a;
            }
        """ parsesInto """
            function () {
                var a_0;
                try {} catch (a_1) { a_1 }
                a_0;
            }
        """ transformsInto """
            function () {
                var a_0;
                try {} catch (a_1) { a_1 }
                a_0;
            }
        """
    }

    private infix fun String.parsesInto(result: String): String {
        assertEquals(result.parse().toString(), parse().makeNamesUnique().toString(), "Unexpected parsing result")
        return result
    }

    private infix fun String.transformsInto(result: String): String {
        val node = toAst()
        node.fixForwardNameReferences()
        node.makeNamesUnique()

        assertEquals(result.parse().toString(), node.toString(), "Unexpected transformation result")
        return result
    }

}

private fun String.parse(): JsNode = parse(this, ThrowExceptionOnErrorReporter, JsProgram().scope, "test")!!.single()

private fun JsNode.transformEachName(transform: JsName.() -> JsName): JsNode {
    accept(object : RecursiveJsVisitor() {
        override fun visitFunction(x: JsFunction) {
            x.name?.let { x.name = it.transform() }
            super.visitFunction(x)
        }

        override fun visitNameRef(nameRef: JsNameRef) {
            nameRef.name?.let { nameRef.name = it.transform() }
            super.visitNameRef(nameRef)
        }

        override fun visitParameter(x: JsParameter) {
            x.name = x.name.transform()
            super.visitParameter(x)
        }

        override fun visitLabel(x: JsLabel) {
            x.name = x.name.transform()
            super.visitLabel(x)
        }

        override fun visit(x: JsVars.JsVar) {
            x.name = x.name.transform()

            super.visit(x)
        }
    })

    return this
}

private fun String.toAst(): JsNode {
    val nameMapping = mutableMapOf<String, Pair<JsName, JsName>>()

    return parse().transformEachName {
        nameMapping[ident]?.let { (oldName, newName) ->
            if (oldName != this) error("Duplicate unique name: ${ident}")
            newName
        } ?: run {

            val parts = ident.split('_')
            if (parts.size != 2) error("Unable to parse: $ident")
            val (name, index) = parts
            if (ident != "${name}_$index") error("Unable to parse: $ident")

            JsName(name, false).also {
                nameMapping[ident] = this to it
            }
        }
    }
}

private fun JsNode.makeNamesUnique(): JsNode {

    val nameCounter = mutableMapOf<String, Int>()
    val nameMap = mutableMapOf<JsName, JsName>()

    return transformEachName {
        nameMap[this] ?: run {
            val index = nameCounter.getOrDefault(ident, 0).also {
                nameCounter[ident] = it + 1
            }

            JsName("${ident}_$index", false).also {
                nameMap[this] = it
            }
        }
    }
}