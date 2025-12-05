/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.impl

import kotlin.script.experimental.api.*

fun ScriptCompilationConfiguration.refineOnSyntaxTree(
    script: SourceCode,
    collectData: () -> ScriptCollectedData?
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    refineImplWithLazyDataCollection(
        ScriptCompilationConfiguration.refineConfigurationOnSyntaxTree,
        collectData
    ) { config, refineData, collectedData ->
        refineData.handler.invoke(ScriptConfigurationRefinementContext(script, config, collectedData))
    }

