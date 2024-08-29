/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 *  Configures instrumentation for main and test [JavaCompile] tasks in the project.
 */

// Hide window of instrumentation tasks
val headlessOldValue: String? = System.setProperty("java.awt.headless", "true")
logger.info("Setting java.awt.headless=true, old value was $headlessOldValue")

pluginManager.withPlugin("org.gradle.java") {
    val javaInstrumentator by configurations.creating {
        isCanBeConsumed = false
        isCanBeResolved = false
        isCanBeDeclared = true
        isVisible = false
        defaultDependencies {
            add(project.dependencies.create("com.jetbrains.intellij.java:java-compiler-ant-tasks:${rootProject.extra["versions.intellijSdk"]}"))
        }
    }

    val javaInstrumentatorResolver by configurations.creating {
        isCanBeConsumed = false
        isCanBeResolved = true
        isCanBeDeclared = false
        isVisible = false
        extendsFrom(javaInstrumentator)
    }

    sourceSets
        .matching { it.name == SourceSet.MAIN_SOURCE_SET_NAME || it.name == SourceSet.TEST_SOURCE_SET_NAME }
        .configureEach {
            tasks.named(compileJavaTaskName, InstrumentJava(javaInstrumentator.incoming.files))
        }
}
