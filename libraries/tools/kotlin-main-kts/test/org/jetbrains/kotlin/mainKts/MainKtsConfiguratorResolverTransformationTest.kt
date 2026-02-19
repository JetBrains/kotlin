/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts

import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver

class MainKtsConfiguratorResolverTransformationTest {
    @Test
    fun test() {
        val resolverA = FileSystemDependenciesResolver()
        val resolverB = FileSystemDependenciesResolver()
        val configuratorA = MainKtsConfigurator(resolverA)
        configuratorA.checkResolver { assertEquals(it, resolverA) }

        val configuratorB = configuratorA.transformResolver { resolverB }
        configuratorB.checkResolver { assertEquals(it, resolverB) }
    }

    private inline fun MainKtsConfigurator.checkResolver(
        crossinline check: (ExternalDependenciesResolver) -> Unit,
    ) = transformResolver {
        check(it)
        it
    }
}
