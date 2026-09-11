/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SyntheticPackageChangeReport
import java.io.File

/**
 * A [SyntheticPackageChangeReport] snapshot of every file under [root], used to compare the Swift Export
 * outputs of two targets. A missing [root] snapshots as empty.
 */
internal fun snapshotDirectory(root: File): SyntheticPackageChangeReport.Snapshot =
    SyntheticPackageChangeReport.snapshot(root, root.walkTopDown().filter { it.isFile }.toList())

/**
 * Renders the [changes] between the Swift Export output at [primary] (the one the Swift package is generated
 * from) and the Swift Export output at [other] as a Gradle warning message.
 */
internal fun renderSwiftExportOutputDivergence(
    primary: File,
    other: File,
    changes: SyntheticPackageChangeReport.Changes,
): String = buildString {
    append(
        "Swift Export output at $other differs from the output at $primary that the Swift package is " +
                "generated from. The package will only contain the latter."
    )
    changes.added.forEach { append("\n  + $it (only in $other)") }
    changes.removed.forEach { append("\n  - $it (missing in $other)") }
    changes.modified.forEach { append("\n  ~ $it (modified)") }
}
