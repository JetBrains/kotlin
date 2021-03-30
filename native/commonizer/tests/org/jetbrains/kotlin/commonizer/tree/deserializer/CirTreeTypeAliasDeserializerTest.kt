/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("RemoveRedundantQualifierName")

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirRegularTypeProjection
import org.jetbrains.kotlin.commonizer.cir.CirTypeAliasType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class CirTreeTypeAliasDeserializerTest : AbstractCirTreeDeserializerTest() {

    fun `test simple type alias`() {
        val module = createCirTreeFromSourceCode("typealias x = Int")
        val typeAlias = module.assertSingleTypeAlias().typeAlias
        assertEquals(CirName.create("x"), typeAlias.name, "Expected correct type alias name")
        assertEquals("kotlin/Int", typeAlias.expandedType.toString())
        assertEquals("kotlin/Int", typeAlias.underlyingType.toString())
    }

    fun `test transitive type alias`() {
        val module = createCirTreeFromSourceCode(
            """
                interface Map<K, V>
                typealias IntMap<V> = Map<Int, V>
                typealias IntStringMap = IntMap<String>
            """.trimIndent()
        )
        val pkg = module.assertSinglePackage()
        assertEquals(2, pkg.typeAliases.size, "Expected exactly 2 type aliases in package")

        val intStringMapTypeAlias = pkg.typeAliases.singleOrNull { it.typeAlias.name.toStrippedString() == "IntStringMap" }
            ?: kotlin.test.fail("Missing 'IntStringMap' type alias")

        val underlyingType = assertIs<CirTypeAliasType>(
            intStringMapTypeAlias.typeAlias.underlyingType, "Expected underlying type to be type alias"
        )

        assertEquals("/IntMap", underlyingType.classifierId.toString())
        val underlyingTypeArgument = underlyingType.arguments.singleOrNull()
            ?: kotlin.test.fail("Expected single argument on underlying type. Found ${underlyingType.arguments}}")

        assertIs<CirRegularTypeProjection>(underlyingTypeArgument, "Expected regular type projection for underlying type")
        assertEquals("kotlin/String", underlyingTypeArgument.type.toString())


        val expandedType = assertIs<CirClassType>(intStringMapTypeAlias.typeAlias.expandedType)
        assertFalse(expandedType.isMarkedNullable, "Expected expanded type to be *not* marked nullable")

        kotlin.test.assertEquals("/Map", expandedType.classifierId.toString(), "Expected correct expanded type classifier")
        kotlin.test.assertEquals(2, expandedType.arguments.size, "Expected two type arguments")
        kotlin.test.assertEquals("kotlin/Int", expandedType.arguments[0].toString(), "Expected correct first type argument")
        kotlin.test.assertEquals("kotlin/String", expandedType.arguments[1].toString(), "Expected correct second type argument")
    }
}
