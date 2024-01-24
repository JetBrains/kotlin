/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.util

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.analysis.test.api.analysis.TestAnalysisContext
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestProject
import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.KotlinJvmTestProject

/**
 * @return the only existing source set or an exception
 */
fun TestAnalysisContext.singleSourceSet(): DokkaConfiguration.DokkaSourceSet {
    return this.configuration.sourceSets.single()
}

fun TestAnalysisContext.defaultKotlinSourceSet() = findSourceSetById(KotlinJvmTestProject.DEFAULT_SOURCE_SET_ID)
fun TestAnalysisContext.defaultJavaSourceSet() = findSourceSetById(JavaTestProject.DEFAULT_SOURCE_SET_ID)

fun TestAnalysisContext.findSourceSetById(dokkaSourceSetID: DokkaSourceSetID): DokkaConfiguration.DokkaSourceSet {
    return this.configuration.sourceSets.single {
        it.sourceSetID == dokkaSourceSetID
    }
}
