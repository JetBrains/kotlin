/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Bound
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.TypeAliased
import org.jetbrains.dokka.utilities.cast
import utils.AbstractModelTest
import utils.name
import kotlin.test.Test

class NestedTypealiasesTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "classes") {

    private fun Bound.checkType(
        expectedName: String,
        underlyingName: String,
    ) = with(this) {
        val ta = this.cast<TypeAliased>()
        ta.typeAlias.name equals expectedName
        ta.inner.name equals underlyingName
    }

    private fun <T : Documentable> T.checkTypeAlias(
        expectedName: String,
        expectedType: String,
        expectedUnderlyingType: String
    ) = with(this) {
        val ta = this.cast<DTypeAlias>()
        ta.name equals expectedName
        ta.dri equals DRI("classes", "Foo.$expectedName")
        ta.type.name equals expectedType
        ta.underlyingType.values.single().name equals expectedUnderlyingType
    }

    @Test
    fun `test nested typealias`() {
        inlineModelTest(
            """
        |interface Foo {
        |    typealias A = String
        |
        |    val property: A
        |
        |    fun A.extension(): Unit {}
        |    fun parameter(a: A): Unit {}
        |    fun returnValue(): A {
        |        return ""
        |    }
        |}
            """
        ) {
            with((this / "classes" / "Foo").cast<DInterface>()) {
                this.typealiases counts 1

                val ta = this.typealiases.first()
                ta.checkTypeAlias("A", "Foo.A", "String")

                val property = this.properties.single()
                property.type.checkType("Foo.A", "String")

                this.functions.find { it.name == "extension" }!!.receiver!!.type.checkType("Foo.A", "String")
                this.functions.find { it.name == "parameter" }!!.parameters.single().type.checkType("Foo.A", "String")
                this.functions.find { it.name == "returnValue" }!!.type.checkType("Foo.A", "String")
            }
        }
    }
}