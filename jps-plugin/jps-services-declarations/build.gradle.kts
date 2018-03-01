/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/**
 * A hacky module to provide Intellij services declarations,
 * so Kotlin plugin can be compiled even when service API
 * is not present in all Intellij builds we support.
 */

plugins { java }

dependencies {
    compileOnly(intellijDep()) { includeJars("annotations", "util") }
    compileOnly(intellijDep("jps-standalone")) { includeJars("jps-builders") }
}

sourceSets {
    "main" { projectDefault() }
}
