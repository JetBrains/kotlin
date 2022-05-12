/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("compile-to-bitcode")
}

bitcode {
    module("files")
    module("env")
}

val hostName: String by project

val build by tasks.registering {
    dependsOn("${hostName}Common")
}

val clean by tasks.registering {
    doFirst {
        delete(buildDir)
    }
}
