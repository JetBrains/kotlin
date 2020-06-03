/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizationPerformed
import org.jetbrains.kotlin.descriptors.commonizer.Result
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.test.util.DescriptorValidator.*
import org.jetbrains.kotlin.types.ErrorUtils
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertFalse
import kotlin.test.fail

fun assertIsDirectory(file: File) {
    if (!file.isDirectory)
        fail("Not a directory: $file")
}

@ExperimentalContracts
fun assertCommonizationPerformed(result: Result) {
    contract {
        returns() implies (result is CommonizationPerformed)
    }

    if (result !is CommonizationPerformed)
        fail("$result is not instance of ${CommonizationPerformed::class}")
}

@ExperimentalContracts
fun assertModulesAreEqual(expected: ModuleDescriptor, actual: ModuleDescriptor, designatorMessage: String) {
    val visitor = ComparingDeclarationsVisitor(designatorMessage)
    val context = visitor.Context(actual)

    expected.accept(visitor, context)
}

fun assertValidModule(module: ModuleDescriptor) = validate(
    object : ValidationVisitor() {
        override fun validateScope(scopeOwner: DeclarationDescriptor?, scope: MemberScope, collector: DiagnosticCollector) = Unit

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, collector: DiagnosticCollector): Boolean {
            assertValid(descriptor)
            return super.visitModuleDeclaration(descriptor, collector)
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, collector: DiagnosticCollector): Boolean {
            assertValid(descriptor)
            return super.visitClassDescriptor(descriptor, collector)
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, collector: DiagnosticCollector): Boolean {
            assertValid(descriptor)
            return super.visitFunctionDescriptor(descriptor, collector)
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, collector: DiagnosticCollector): Boolean {
            assertValid(descriptor)
            return super.visitPropertyDescriptor(descriptor, collector)
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, collector: DiagnosticCollector): Boolean {
            assertValid(constructorDescriptor)
            return super.visitConstructorDescriptor(constructorDescriptor, collector)
        }
    },
    module
)

@Suppress("NOTHING_TO_INLINE")
private inline fun assertValid(descriptor: DeclarationDescriptor) = when (descriptor) {
    is ModuleDescriptor -> descriptor.assertValid()
    else -> assertFalse(ErrorUtils.isError(descriptor), "$descriptor is error")
}
