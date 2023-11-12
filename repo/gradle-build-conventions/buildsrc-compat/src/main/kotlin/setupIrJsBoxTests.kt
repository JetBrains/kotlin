/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

fun Test.useJsIrBoxTests(
    version: Any,
    buildDir: Provider<Directory>,
    fullStdLib: String = "libraries/stdlib/build/classes/kotlin/js/main",
    reducedStdlibPath: String = "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main",
    kotlinJsTestPath: String = "libraries/kotlin.test/build/classes/kotlin/js/main",
    domApiCompatPath: String = "libraries/kotlin-dom-api-compat/build/classes/kotlin/main"
) {
    setupV8()
    dependsOn(":kotlin-stdlib:jsJar")
    dependsOn(":kotlin-stdlib:jsJarForTests") // TODO: think how to remove dependency on the artifact in this place
    dependsOn(":kotlin-test:jsJar")
    dependsOn(":kotlin-test:compileKotlinJs")
    dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
    dependsOn(":kotlin-dom-api-compat:compileKotlinJs")

    systemProperty("kotlin.js.test.root.out.dir", "${buildDir.get().asFile}/")
    systemProperty("kotlin.js.full.stdlib.path", fullStdLib)
    systemProperty("kotlin.js.reduced.stdlib.path", reducedStdlibPath)
    systemProperty("kotlin.js.kotlin.test.path", kotlinJsTestPath)
    systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib")
    systemProperty("kotlin.js.kotlin.test.klib.path", "libraries/kotlin.test/build/libs/kotlin-test-js-$version.klib")
    systemProperty("kotlin.js.dom.api.compat", domApiCompatPath)
}
