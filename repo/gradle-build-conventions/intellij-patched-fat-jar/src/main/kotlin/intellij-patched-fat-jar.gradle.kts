/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.tasks.Jar

/*
 * Convention plugin for the `:dependencies:intellij-*` modules that wrap IntelliJ artifacts.
 *
 * The plugin produces deduplicated fat-JARs, combining content of IntelliJ IDEA Maven artifacts and
 * patches versions of selected classes (necessary to overcome various compatibility issues).
 *
 * As 'jar' and 'sourcesJar' contain deduplicated classes/sources, consumers of ':dependencies:intellij-*' projects,
 * including `intellijCore()`, simply receive merged, patched fat-JARs.
 */

configure<JavaPluginExtension> {
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    addEmbeddedRuntime()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    addEmbeddedLibrarySources(configurations["embedded"])
}

configurations.named("apiElements") {
    /*
        For compile avoidance, 'apiElements' publishes 'classes' and 'resources' variants.
        However, those variants only include content of the module itself (without its dependencies), so consumers compiling
        against the module will (by default) get the incorrect classpath. This is a per-module equivalent of Gradle's
        `org.gradle.java.compile-classpath-packaging`.

        `runtimeElements` isn't changed a runtime classpath resolution already selects the proper JAR variant.
    */
    val removed = outgoing.variants.removeIf { it.name == "classes" || it.name == "resources" }
    check(removed) { "'classes' and 'resources' variants expected" }
}
