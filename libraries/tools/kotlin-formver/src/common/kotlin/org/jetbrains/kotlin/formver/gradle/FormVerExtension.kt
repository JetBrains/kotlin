/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.gradle


open class FormVerExtension {
    internal var myLogLevel: String? = null
    internal var myErrorStyle: String? = null
    internal var myUnsupportedFeatureBehaviour: String? = null
    internal var myConversionTargetsSelection: String? = null
    internal var myVerificationTargetsSelection: String? = null

    open fun logLevel(logLevel: String) {
        myLogLevel = logLevel
    }

    open fun errorStyle(style: String) {
        myErrorStyle = style
    }

    open fun unsupportedFeatureBehaviour(behaviour: String) {
        myUnsupportedFeatureBehaviour = behaviour
    }

    open fun conversionTargetsSelection(selection: String) {
        myConversionTargetsSelection = selection
    }

    open fun verificationTargetsSelection(selection: String) {
        myVerificationTargetsSelection = selection
    }
}