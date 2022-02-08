/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree

import org.jetbrains.kotlin.commonizer.tree.deserializer.*

internal val defaultCirTreeModuleDeserializer = CirTreeModuleDeserializer(
    packageDeserializer = CirTreePackageDeserializer(
        propertyDeserializer = CirTreePropertyDeserializer,
        functionDeserializer = CirTreeFunctionDeserializer,
        typeAliasDeserializer = CirTreeTypeAliasDeserializer,
        classDeserializer = CirTreeClassDeserializer(
            propertyDeserializer = CirTreePropertyDeserializer,
            functionDeserializer = CirTreeFunctionDeserializer,
            constructorDeserializer = CirTreeClassConstructorDeserializer
        )
    )
)

internal val defaultCirTreeRootDeserializer = RootCirTreeDeserializer(
    defaultCirTreeModuleDeserializer
)
