/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.ui.layout.panel
import org.jetbrains.kotlin.idea.KotlinBundle
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class KotlinSuspendingCallHintsProvider : KotlinAbstractHintsProvider<KotlinSuspendingCallHintsProvider.Settings>() {

    data class Settings(var suspendingCalls: Boolean = false)

    override val name: String = KotlinBundle.message("hints.settings.suspending")

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel {}

            override val mainCheckboxText: String = KotlinBundle.message("hints.settings.common.items")

            override val cases: List<ImmediateConfigurable.Case>
                get() = listOf(
                    ImmediateConfigurable.Case(
                        KotlinBundle.message("hints.settings.suspending"),
                        "hints.suspending.calls",
                        settings::suspendingCalls
                    )
                )
        }
    }

    override fun createSettings(): Settings = Settings()

    override fun isElementSupported(resolved: HintType?, settings: Settings): Boolean {
        return when (resolved) {
            HintType.SUSPENDING_CALL -> settings.suspendingCalls
            else -> false
        }
    }
}