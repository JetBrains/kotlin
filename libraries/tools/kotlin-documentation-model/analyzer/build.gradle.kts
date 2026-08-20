/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.utils.downloadLatestKotlinStdlibJvmSources
import dokkabuild.utils.systemProperty
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.test-k2")
    `java-test-fixtures`
    idea
}

dependencies {

    // Other
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jackson.kotlin)

    // Test only
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiterParams)
/// markdown
    implementation(libs.jsoup)
    implementation(libs.jetbrains.markdown)

//Java
    // This must be explicit so the full `java-psi` API takes precedence over
    // stripped copies that may be present in compiler-related artifacts.

    // this is a `hack` to include classes `intellij-java-psi-api` in shadowJar
    // which are not present in `kotlin-compiler`
    // it's harder to do it in the same as with `fastutil`
    // as several intellij dependencies share the same packages like `org.intellij.core`
    api(libs.intellij.java.psi.api) { isTransitive = false }

    // The syntax artifacts are already available transitively via `java-psi(-impl)`, but `kotlin-compiler`
    // bundles stripped copies of them: their Kotlin metadata is kept, while the static fields backing
    // `const val`s and the synthetic `$annotations` methods holding `@JvmStatic` are pruned. Compiling against
    // such a copy either fails outright (internal compiler error on `ReferenceParser.EAT_LAST_DOT`) or produces
    // bytecode that doesn't match the real artifact used at runtime (`IncompatibleClassChangeError` for
    // `SyntaxTokenTypes.WHITE_SPACE`) — see the vendored `JavaDocParser`. Declaring them explicitly puts them
    // before `kotlin-compiler` on the compile classpath, which is the order the runtime classpath already has.
    implementation(libs.intellij.java.syntax)
    implementation(libs.intellij.platform.syntax)
    implementation(libs.intellij.platform.syntax.util)

    // We exclude `log4j` as it's not used in our codebase,
    // and we do override intellij logger with NOOP logger
    // `log4j` dependency triggers errors by dependency vulnerability checkers
    implementation(libs.intellij.java.psi.impl) {
        exclude("org.jetbrains.intellij.deps", "log4j")
    }
    // Since intellij-platform 261, core PSI API classes such as `com.intellij.psi.PsiElement` are no longer
    // on the `java-psi(-impl)` classpath. They're needed only at compile time here — at runtime they're
    // provided by `kotlin-compiler` (which bundles the IntelliJ core).
    compileOnly(libs.intellij.platform.core)
    implementation(libs.intellij.util)

    // ----------- Analysis dependencies ----------------------------------------------------------------------------

    listOf(
        libs.kotlin.analysis.api.api,
        libs.kotlin.analysis.api.standalone,
    ).forEach {
        implementation(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    listOf(
        libs.kotlin.analysis.api.impl,
        libs.kotlin.analysis.api.fir,
        libs.kotlin.low.level.api.fir,
        libs.kotlin.analysis.api.platform,
        libs.kotlin.symbol.light.classes,
        // provides `org.jetbrains.kotlin.analysis.decompiler.*` classes (e.g. ClsKotlinBinaryClassCache),
        // which since 2.4.20-dev-5364 are no longer bundled in `kotlin-compiler`
        libs.kotlin.compiler.k2.common,
    ).forEach {
        runtimeOnly(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    // copy-pasted from Analysis API https://github.com/JetBrains/kotlin/blob/a10042f9099e20a656dec3ecf1665eea340a3633/analysis/low-level-api-fir/build.gradle.kts#L37
    runtimeOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")

    implementation(libs.kotlin.compiler.k2) {
        isTransitive = false
    }
}

tasks.test {
    maxHeapSize = "4G"
}


//region Download and unpack kotlin-stdlib, so EnumTemplatesTest can test synthetic enum functions.
val kotlinStdlibSourcesDir = downloadLatestKotlinStdlibJvmSources(project)
tasks.withType<Test>().configureEach {
    systemProperty
        .inputDirectory("kotlinStdlibSourcesDir", kotlinStdlibSourcesDir)
        .withPathSensitivity(RELATIVE)
}
//endregion

tasks.wrapper {
    doLast {
        // Manually update the distribution URL to use cache-redirector.
        // (Workaround for https://github.com/gradle/gradle/issues/17515)
        propertiesFile.writeText(
            propertiesFile.readText()
                .replace(
                    "https\\://services.gradle.org/",
                    "https\\://cache-redirector.jetbrains.com/services.gradle.org/",
                )
        )
    }
}

idea {
    module {
        // Mark directories as excluded so that they don't appear in IntelliJ's global search.
        excludeDirs.addAll(
            files(
                ".idea",
                ".husky",
                ".kotlin",
            )
        )
    }
}
