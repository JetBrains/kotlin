/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

// When artifacts are resolved from a remote-layout Maven repo (e.g. build/repo), unique-snapshot
// versions carry a timestamp qualifier appended by the repo:
//   coordinates: "group:name:x.y-SNAPSHOT:yyyyMMdd.HHmmss-n"
//   jar filename: "foo-x.y-yyyyMMdd.HHmmss-n.jar"
// These normalizers strip that qualifier so test assertions stay stable regardless of which repo
// layout is in use (build/repo remote layout vs mavenLocal non-unique layout).

private val uniqueSnapshotCoordinateQualifier = Regex(":[0-9]{8}\\.[0-9]{6}-[0-9]+$")
private val uniqueSnapshotJarQualifier = Regex("-[0-9]{8}\\.[0-9]{6}-[0-9]+(\\.[a-z]+)$")

/** Strips the `:yyyyMMdd.HHmmss-n` timestamp from a resolved Maven coordinate string. */
internal fun String.normalizeSnapshotVersion(): String =
    uniqueSnapshotCoordinateQualifier.replace(this, "")

/** Normalises a jar filename: replaces `-yyyyMMdd.HHmmss-n.ext` with `-SNAPSHOT.ext`. */
internal fun String.normalizeSnapshotJarName(): String =
    uniqueSnapshotJarQualifier.replace(this, "-SNAPSHOT$1")
