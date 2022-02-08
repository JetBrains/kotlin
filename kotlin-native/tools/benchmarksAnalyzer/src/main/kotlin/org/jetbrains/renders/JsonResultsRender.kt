/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.*

// Report render to text format.
class JsonResultsRender: Render() {
    override val name: String
        get() = "json"

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean) =
            report.getBenchmarksReport().toJson()
}