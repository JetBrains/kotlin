/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class ParcelizeIrBoxTestWithSerializableLikeExtension : AbstractParcelizeIrBoxTest() {
    fun testSimple() = doTest("plugins/parcelize/parcelize-compiler/testData/box/simple.kt")

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        super.setupEnvironment(environment)
        SyntheticResolveExtension.registerExtension(environment.project, SerializableLike())
    }

    private class SerializableLike : SyntheticResolveExtension {
        override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
            fun ClassDescriptor.isSerializableLike() = annotations.hasAnnotation(FqName("test.SerializableLike"))

            return when {
                thisDescriptor.kind == ClassKind.CLASS && thisDescriptor.isSerializableLike() -> Name.identifier("Companion")
                else -> return null
            }
        }
    }
}
