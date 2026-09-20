/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testInventory

import org.junit.jupiter.api.Test

/**
 * What the fixture's second test task runs, and all it runs, see this module's ReadMe. One plain
 * test is enough: covering the shapes a recording can take is [TestInventoryFixtureTest]'s job. Its
 * name is asserted verbatim just the same.
 */
class SecondTestInventoryFixtureTest {

    @Test
    fun recordedBySecondTestTask() = Unit
}
