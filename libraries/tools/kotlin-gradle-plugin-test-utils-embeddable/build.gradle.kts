/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Shaded test jars from compiler for Gradle integration tests"

plugins { `java` }

val packedJars by configurations.creating

val projectsToInclude = listOf(":compiler:tests-common",
                               ":compiler:incremental-compilation-impl",
                               ":kotlin-build-common")

dependencies {
    for (projectName in projectsToInclude) {
        compile(projectTests(projectName)) { isTransitive = false }
        packedJars(projectTests(projectName)) { isTransitive = false }
    }

    packedJars(intellijDep()) { includeJars("idea_rt") }

}

runtimeJar(rewriteDepsToShadedCompiler(
    task<ShadowJar>("shadowJar")  {
        from(packedJars)
    }
))
