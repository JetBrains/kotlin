/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.TestProject
import kotlin.io.path.appendText
import kotlin.io.path.name
import kotlin.io.path.walk

class Kapt4IT : Kapt3IT(languageVersion = "2.0") {
    override fun TestProject.customizeProject() {
        forceKapt4()
    }
}

fun TestProject.forceKapt4() {
    projectPath.walk().forEach {
        if (it.fileName.name == "build.gradle") {
            it.appendText("""
                
            try {
                compileKotlin.configure {
                    kotlinOptions.freeCompilerArgs += "-Xuse-kapt4"
                    kotlinOptions.freeCompilerArgs += "-Xsuppress-version-warnings"
                }
            } catch(Exception e) {}
            try {
                kaptGenerateStubsKotlin {
                    kotlinOptions.freeCompilerArgs += "-Xuse-kapt4"
                    kotlinOptions.freeCompilerArgs += "-Xsuppress-version-warnings"
                } 
            } catch(Exception e) {}           
            """.trimIndent())
        }
    }
}