/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("benchmarking")
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("src/main/kotlin")
        }
        nativeMain {
            kotlin.srcDir("src/main/kotlin-native")
        }
    }
}

benchmark {
    applicationName = "Ring"
}
