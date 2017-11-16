/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin IDEA plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

plugins {
    `java-base`
}

val projectsToShadow = listOf(
        ":plugins:annotation-based-compiler-plugins-ide-support",
        ":compiler:backend",
        ":compiler:backend-common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":compiler:daemon-common",
        ":core",
        ":eval4j",
        ":idea:formatter",
        ":compiler:frontend",
        ":compiler:frontend.java",
        ":compiler:frontend.script",
        ":idea:ide-common",
        ":idea",
        ":idea:idea-android",
        ":idea:idea-android-output-parser",
        ":idea:idea-core",
        ":idea:idea-jvm",
        ":idea:idea-jps-common",
        ":idea:idea-gradle",
        ":idea:idea-maven",
        //":idea-ultimate",
        ":compiler:ir.psi2ir",
        ":compiler:ir.tree",
        ":j2k",
        ":js:js.ast",
        ":js:js.frontend",
        ":js:js.parser",
        ":js:js.serializer",
        ":compiler:light-classes",
        ":compiler:plugin-api",
        ":kotlin-preloader",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:util",
        ":core:util.runtime")

val packedJars by configurations.creating
val sideJars by configurations.creating

dependencies {
    packedJars(preloadedDeps("protobuf-${rootProject.extra["versions.protobuf-java"]}"))
    packedJars(project(":kotlin-stdlib", configuration = "builtins"))
    sideJars(projectDist(":kotlin-script-runtime"))
    sideJars(projectDist(":kotlin-stdlib"))
    sideJars(projectDist(":kotlin-reflect"))
    sideJars(commonDep("io.javaslang", "javaslang"))
    sideJars(commonDep("javax.inject"))
    sideJars(preloadedDeps("markdown", "kotlinx-coroutines-core", "kotlinx-coroutines-jdk8"))
}

val jar = runtimeJar(task<ShadowJar>("shadowJar")) {
    from(files("$rootDir/resources/kotlinManifest.properties"))
    from(packedJars)
    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getSourceSetsFrom(p)["main"].output)
    }
    archiveName = "kotlin-plugin.jar"
}

ideaPlugin {
    shouldRunAfter(":dist")
    from(jar)
    from(sideJars)
}

