/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import kotlin.metadata.KmTypeAlias
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.tree.CirTreeTypeAlias


internal object CirTreeTypeAliasDeserializer {
    operator fun invoke(typeAlias: KmTypeAlias, packageName: CirPackageName, typeResolver: CirTypeResolver): CirTreeTypeAlias {
        val typeAliasTypeResolver = typeResolver.create(typeAlias.typeParameters)
        val typeAliasName = CirName.create(typeAlias.name)
        return CirTreeTypeAlias(
            id = CirEntityId.create(packageName, typeAliasName),
            typeAlias = CirDeserializers.typeAlias(
                name = typeAliasName,
                source = typeAlias,
                typeResolver = typeAliasTypeResolver
            )
        )
    }
}

