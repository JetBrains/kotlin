/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import org.jetbrains.kotlinx.serialization.compiler.resolve.SpecialBuiltins

val enumSerializerId = ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.enumSerializer))
val polymorphicSerializerId = ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.polymorphicSerializer))
val referenceArraySerializerId = ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.referenceArraySerializer))
val objectSerializerId = ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.objectSerializer))
val sealedSerializerId = ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.sealedSerializer))
val contextSerializerId = ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.contextSerializer))
