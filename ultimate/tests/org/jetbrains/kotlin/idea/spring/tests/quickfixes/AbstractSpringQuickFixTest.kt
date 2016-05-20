package org.jetbrains.kotlin.idea.spring.tests.quickfixes

import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.idea.spring.tests.configureSpringFileSetByDirective

abstract class AbstractSpringQuickFixTest : AbstractQuickFixTest() {
    override fun configExtra(options: String) {
        configureSpringFileSetByDirective(getModule(), options, listOf(getFile()))
    }
}