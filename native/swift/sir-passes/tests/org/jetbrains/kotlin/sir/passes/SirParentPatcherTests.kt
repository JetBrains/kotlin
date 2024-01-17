/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes

import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.builder.buildEnum
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.sir.passes.utility.PatchDeclarationParentVisitor
import org.jetbrains.sir.passes.utility.SirValidatorConfig
import org.jetbrains.sir.passes.utility.ValidationError
import org.jetbrains.sir.passes.utility.validate
import kotlin.test.Test
import kotlin.test.assertTrue

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class SirParentPatcherTests {
    @Test
    fun `parent patcher should patch parents`() {
        val wrongEnum = buildEnum { name = "wrongEnum" }
        val function = buildFunction {
            name = "foo"
            returnType = SirNominalType(SirSwiftModule.bool)
            isStatic = false
        }
        function.parent = wrongEnum
        val wrongModule = buildModule { name = "wrongModule" }
        val enum = buildEnum {
            name = "e"
            declarations += function
        }
        enum.parent = wrongModule
        val module = buildModule {
            name = "MyModule"
            declarations += enum
        }
        val validationErrorsBefore = validate(module, SirValidatorConfig())
        assertTrue(validationErrorsBefore.filterIsInstance<ValidationError.WrongParent>().count() == 2)
        module.accept(PatchDeclarationParentVisitor(), null)
        val validationErrorsAfter = validate(module, SirValidatorConfig())
        assertTrue(validationErrorsAfter.filterIsInstance<ValidationError.WrongParent>().isEmpty())
    }
}