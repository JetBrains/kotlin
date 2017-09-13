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


import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Android Extensions Compiler"

apply { plugin("kotlin") }

dependencies {
    compileOnly(ideaSdkCoreDeps("intellij-core"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
    runtime("com.google.android:android:2.3.1")
}

//val originalSrc = "$projectDir/src"
//val targetSrc = file("$buildDir/embeddable-target-src")
//
//if (System.getProperty("idea.active") == null) {
//
//    val prepareEmbeddableSources by task<Copy> {
//        from(originalSrc)
//        into(targetSrc)
//        filter { it.replace(Regex("(?<!\\.)com\\.intellij"), "org.jetbrains.kotlin.com.intellij") }
//    }
//
//    tasks.withType<KotlinCompile> { dependsOn(prepareEmbeddableSources) }
//    tasks.withType<JavaCompile> { dependsOn(prepareEmbeddableSources) }
//}


sourceSets {
    "main" {
//        if (System.getProperty("idea.active") == null) {
//            java.srcDir(targetSrc)
//            resources.srcDir("src").apply { include("META-INF/**", "**/*.properties") }
//        }
//        else {
            projectDefault()
//        }
    }
    "test" {}
}

runtimeJar {
    from(getSourceSetsFrom(":kotlin-android-extensions-runtime")["main"].output.classesDirs)
}

sourcesJar()
javadocJar()

publish()
