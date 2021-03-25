/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.CommonizerParameters
import org.jetbrains.kotlin.commonizer.TargetProvider
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot


private val defaultRootCirTreeDeserializer = RootCirTreeDeserializer(
    ModuleCirTreeDeserializer(
        packageDeserializer = PackageCirTreeDeserializer(
            propertyDeserializer = PropertyCirTreeDeserializer,
            functionDeserializer = FunctionCirTreeDeserializer,
            typeAliasDeserializer = TypeAliasCirTreeDeserializer,
            classDeserializer = ClassCirTreeDeserializer(
                propertyDeserializer = PropertyCirTreeDeserializer,
                functionDeserializer = FunctionCirTreeDeserializer,
                constructorDeserializer = ClassConstructorCirTreeDeserializer
            )
        )
    )
)

internal fun deserialize(parameters: CommonizerParameters, target: TargetProvider): CirTreeRoot {
    return defaultRootCirTreeDeserializer(parameters, target)
}
