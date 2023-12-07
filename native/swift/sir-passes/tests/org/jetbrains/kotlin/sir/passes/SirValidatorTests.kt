/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes

import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.builder.buildForeignFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.mock.MockFunction
import org.jetbrains.kotlin.sir.mock.MockKotlinType
import org.jetbrains.sir.passes.utility.PatchDeclarationParentVisitor
import org.jetbrains.sir.passes.utility.SirValidatorConfig
import org.jetbrains.sir.passes.utility.ValidationError
import org.jetbrains.sir.passes.utility.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SirValidatorTests {
    @Test
    fun `validator should check foreign origins`() {
        val module = buildModule {
            name = "MyModule"
            declarations += buildForeignFunction {
            }
        }

        val config = SirValidatorConfig(checkParents = false)
        val error = validate(module, config).first()
        assertTrue(error is ValidationError.WrongForeignDeclarationOrigin)
        assertEquals(module.declarations.first(), error.declaration)
    }

    @Test
    fun `validatior should check declaration parents`() {
        val wrongModule = buildModule {
            name = "WrongModule"
        }
        val foreignFunction = buildForeignFunction {
            val kotlinEntity = MockFunction(
                fqName = listOf("foo"),
                parameters = emptyList(),
                returnType = MockKotlinType("kotlin/Byte")
            )
            origin = kotlinEntity
        }
        foreignFunction.parent = wrongModule
        val module = buildModule {
            name = "MyModule"
            declarations += foreignFunction
        }

        val config = SirValidatorConfig(
            checkForeignDeclarations = false
        )
        val error = validate(module, config).first()
        assertTrue(error is ValidationError.WrongParent)
        assertEquals(foreignFunction, error.declaration)
        assertEquals(module, error.expectedParent)
    }
}