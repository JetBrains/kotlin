/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Later, we can do the same thing for other foreign annotations, too.
val jakartaAnnotationsClasspath: org.gradle.api.artifacts.Configuration by configurations.creating {
    isCanBeDeclared = true
    isCanBeResolved = false
    isCanBeConsumed = false
    isVisible = false
}

val jakartaAnnotationsClasspathResolver: org.gradle.api.artifacts.Configuration by configurations.creating {
    isCanBeDeclared = false
    isCanBeResolved = true
    isCanBeConsumed = false
    isVisible = false
    extendsFrom(jakartaAnnotationsClasspath)
}

tasks.named<Test>("test") {
    val jakartaAnnotationsFiles: FileCollection =
        jakartaAnnotationsClasspathResolver

    inputs.files(jakartaAnnotationsFiles)
        .withPropertyName("jakartaAnnotationsFiles")
        .withNormalizer(ClasspathNormalizer::class)

    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Djakarta.annotations.classpath=${jakartaAnnotationsFiles.asPath}")
    }
}