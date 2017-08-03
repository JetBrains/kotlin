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

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

plugins {
    maven
}

apply { plugin("kotlin") }

val jarContents by configurations.creating
val testRuntimeCompilerJar by configurations.creating
val testStdlibJar by configurations.creating
val testScriptRuntimeJar by configurations.creating
val default by configurations
val archives by configurations

val projectsToInclude = listOf(
        ":compiler:cli-common",
        ":compiler:daemon-common",
        ":compiler:daemon-client")

dependencies {
    val testCompile by configurations
    projectsToInclude.forEach {
        jarContents(project(it)) { isTransitive = false }
        testCompile(project(it))
    }
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntimeCompilerJar(project(":compiler", configuration = "compilerJar"))
    testStdlibJar(project(":kotlin-stdlib", configuration = "mainJar"))
    testScriptRuntimeJar(project(":kotlin-script-runtime", configuration = "mainJar"))
}

val shadowJar by task<ShadowJar> {
    setupRuntimeJar("Kotlin compiler client embeddable")
    baseName = "kotlin-compiler-client-embeddable"
    from(jarContents)
    classifier = ""
}

configureKotlinProjectSources() // no sources
configureKotlinProjectTests("libraries/tools/kotlin-compiler-client-embeddable-test/src", sourcesBaseDir = rootDir)

tasks.withType<Test> {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = File(rootDir, "libraries/tools/kotlin-compiler-client-embeddable-test/src")
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    systemProperty("kotlin.test.script.classpath", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.joinToString(File.pathSeparator))
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    ignoreFailures = true
    systemProperty("idea.is.unit.test", "true")
    systemProperty("compilerJar", testRuntimeCompilerJar.singleFile.canonicalPath)
    systemProperty("stdlibJar", testStdlibJar.singleFile.canonicalPath)
    systemProperty("scriptRuntimeJar", testScriptRuntimeJar.singleFile.canonicalPath)
}

archives.artifacts.let { artifacts ->
    artifacts.forEach {
        if (it.type == "jar") {
            artifacts.remove(it)
        }
    }
}

artifacts.add(default.name, shadowJar.outputs.files.singleFile) {
    builtBy(shadowJar)
    classifier = ""
}

artifacts.add("archives", shadowJar.outputs.files.singleFile) {
    builtBy(shadowJar)
    classifier = ""
}

apply<plugins.PublishedKotlinModule>()

//tasks {
//    "uploadArchives"(Upload::class) {
//
//        val preparePublication by rootProject.tasks
//        dependsOn(preparePublication)
//
//        val username: String? by preparePublication.extra
//        val password: String? by preparePublication.extra
//
//        repositories {
//            withConvention(MavenRepositoryHandlerConvention::class) {
//
//                mavenDeployer {
//                    withGroovyBuilder {
//                        "repository"("url" to uri(preparePublication.extra["repoUrl"]))
//
//                        if (username != null && password != null) {
//                            "authentication"("userName" to username, "password" to password)
//                        }
//                    }
//
//                    pom.project {
//                        withGroovyBuilder {
//                            "licenses" {
//                                "license" {
//                                    "name"("The Apache Software License, Version 2.0")
//                                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                                    "distribution"("repo")
//                                }
//                            }
//                            "name"("${project.group}:${project.name}")
//                            "packaging"("jar")
//                            // optionally artifactId can be defined here
//                            "description"(project.description)
//                            "url"("https://kotlinlang.org/")
//                            "licenses" {
//                                "license" {
//                                    "name"("The Apache License, Version 2.0")
//                                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                                }
//                            }
//                            "scm" {
//                                "url"("https://github.com/JetBrains/kotlin")
//                                "connection"("scm:git:https://github.com/JetBrains/kotlin.git")
//                                "developerConnection"("scm:git:https://github.com/JetBrains/kotlin.git")
//                            }
//                            "developers" {
//                                "developer" {
//                                    "name"("Kotlin Team")
//                                    "organization"("JetBrains")
//                                    "organizationUrl"("https://www.jetbrains.com")
//                                }
//                            }
//                        }
//                    }
//                    pom.whenConfigured {
//                        dependencies.clear()
////                        dependencies.removeIf {
////                            withGroovyBuilder {
////                                "scope"(*emptyArray()) == "test"
////                            }
////                        }
//                    }
//                }
//            }
//        }
//    }
//}
