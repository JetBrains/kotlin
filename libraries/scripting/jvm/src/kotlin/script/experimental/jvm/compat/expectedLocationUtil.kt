/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.compat

import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.location.ScriptExpectedLocation

@Suppress("DEPRECATION")
fun List<ScriptAcceptedLocation>.mapToLegacyExpectedLocations(): List<ScriptExpectedLocation> = map {
    when (it) {
        ScriptAcceptedLocation.Sources -> ScriptExpectedLocation.SourcesOnly
        ScriptAcceptedLocation.Tests -> ScriptExpectedLocation.TestsOnly
        ScriptAcceptedLocation.Libraries -> ScriptExpectedLocation.Libraries
        ScriptAcceptedLocation.Project -> ScriptExpectedLocation.Project
        ScriptAcceptedLocation.Everywhere -> ScriptExpectedLocation.Everywhere
    }
}

@Suppress("DEPRECATION")
fun List<ScriptExpectedLocation>.mapLegacyExpectedLocations(): List<ScriptAcceptedLocation> = map {
    when (it) {
        ScriptExpectedLocation.SourcesOnly -> ScriptAcceptedLocation.Sources
        ScriptExpectedLocation.TestsOnly -> ScriptAcceptedLocation.Tests
        ScriptExpectedLocation.Libraries -> ScriptAcceptedLocation.Libraries
        ScriptExpectedLocation.Project -> ScriptAcceptedLocation.Project
        ScriptExpectedLocation.Everywhere -> ScriptAcceptedLocation.Everywhere
    }
}
