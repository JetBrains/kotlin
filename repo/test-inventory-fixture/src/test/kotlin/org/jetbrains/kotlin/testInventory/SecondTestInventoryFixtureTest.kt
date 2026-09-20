/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testInventory

import org.junit.jupiter.api.Test

/**
 * What the fixture's second test task runs, and all it runs, see this module's ReadMe.
 *
 * The tests of a whole build are replayed by a single build service, each task's under a flow of its
 * own, so the functional tests need a build with a second test task in it. Covering the shapes a
 * recording can take is [TestInventoryFixtureTest]'s job; one plain test is enough here, and its name
 * is asserted verbatim just the same.
 */
class SecondTestInventoryFixtureTest {

    @Test
    fun recordedBySecondTestTask() = Unit
}
