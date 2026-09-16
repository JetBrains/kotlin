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

/*
    IntelliJ's Gradle importer cannot attach sources to a project fat-JAR: a JAR with content beyond the module's own
    outputs is kept in consumers as a bare module library (binaries only), so navigation from IntelliJ classes would
    only lead to decompiled code. During Gradle sync, the project is therefore modeled the plain way instead: the patched
    classes remain module sources, and the wrapped IntelliJ artifacts are exported as ordinary Maven library
    dependencies, for which the IDE downloads and attaches '-sources.jar' itself.

    Regular Gradle builds (including tasks the IDE runs) are not affected, as 'idea.sync.active' is only set while
    the IDE builds its project model.
*/
val isInIdeaSync = kotlinBuildProperties.isInIdeaSync.get()

tasks.named<Jar>("jar") {
    if (!isInIdeaSync) {
        addEmbeddedRuntime()
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

if (isInIdeaSync) {
    configurations.named("api") {
        extendsFrom(configurations["embedded"])
    }
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
