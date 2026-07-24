/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val thirdPartyAnnotationsClasspath = configurations.create("thirdPartyAnnotationsClasspath") {
    isCanBeDeclared = true
    isCanBeResolved = false
    isCanBeConsumed = false
    isTransitive = false
}

val thirdPartyAnnotationsClasspathResolver = configurations.create("thirdPartyAnnotationsClasspathResolver") {
    isCanBeDeclared = false
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
    extendsFrom(thirdPartyAnnotationsClasspath)
}

tasks.named<Test>("test") {
    val annotationsFiles: FileCollection =
        thirdPartyAnnotationsClasspathResolver

    inputs.files(annotationsFiles)
        .withPropertyName("annotationsFiles")
        .withNormalizer(ClasspathNormalizer::class)

    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Dthird.party.annotations.classpath=${annotationsFiles.asPath}")
    }
}