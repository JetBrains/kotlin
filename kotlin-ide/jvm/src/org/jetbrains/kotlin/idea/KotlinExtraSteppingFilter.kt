/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.debugger.NoDataException
import com.intellij.debugger.engine.ExtraSteppingFilter
import com.intellij.debugger.engine.SuspendContext
import com.intellij.debugger.settings.DebuggerSettings
import com.sun.jdi.Location
import com.sun.jdi.request.StepRequest
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.util.application.runReadAction

class KotlinExtraSteppingFilter : ExtraSteppingFilter {
    override fun isApplicable(context: SuspendContext?): Boolean {
        if (context == null) {
            return false
        }

        val debugProcess = context.debugProcess ?: return false
        val positionManager = KotlinPositionManager(debugProcess)
        val location = context.frameProxy?.location() ?: return false
        return runReadAction {
            shouldFilter(positionManager, location)
        }
    }


    private fun shouldFilter(positionManager: KotlinPositionManager, location: Location): Boolean {
        val defaultStrata = location.declaringType()?.defaultStratum()
        if ("Kotlin" != defaultStrata) {
            return false
        }

        val sourcePosition = try {
            positionManager.getSourcePosition(location)
        } catch (e: NoDataException) {
            return false
        } ?: return false

        if (isInSuspendMethod(location) && isOnSuspendReturnOrReenter(location) && !isOneLineMethod(location)) {
            return true
        }

        val settings = DebuggerSettings.getInstance()
        if (settings.TRACING_FILTERS_ENABLED) {
            val classNames = positionManager.originalClassNamesForPosition(sourcePosition).map { it.replace('/', '.') }
            if (classNames.isEmpty()) {
                return false
            }

            for (className in classNames) {
                for (filter in settings.steppingFilters) {
                    if (filter.isEnabled) {
                        if (filter.matches(className)) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    override fun getStepRequestDepth(context: SuspendContext?): Int {
        return StepRequest.STEP_INTO
    }
}

