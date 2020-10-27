/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.test

import spock.lang.Ignore

import java.nio.file.Files
import java.nio.file.Paths

class MultiplatformSpecification extends BaseKonanSpecification {

    public static final String KOTLIN_VERSION = System.getProperty("kotlin.version")
    public static final String KOTLIN_REPO = System.getProperty("kotlin.repo")
    public static final String DEFAULT_COMMON_BUILD_FILE_CONTENT = """\
        buildscript {
            repositories {
                maven {
                  url = '$KOTLIN_REPO'
                }
                maven {
                   url = 'https://cache-redirector.jetbrains.com/jcenter'
                }
                jcenter()
            }
            dependencies {
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION"
            }
        }

        apply plugin: 'kotlin-platform-common'
        
        repositories {
            maven {
               url = '$KOTLIN_REPO'
            }
            maven {
               url = 'https://cache-redirector.jetbrains.com/jcenter'
            }
            jcenter()
        }

        dependencies {
            compile "org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"
        }
        """.stripIndent()

    static File createCommonProject(KonanProject platformProject,
                             String commonProjectName = "common",
                             String commonBuildFileContent = DEFAULT_COMMON_BUILD_FILE_CONTENT) {
        def commonDirectory = platformProject.createSubDir(commonProjectName)
        def commonBuildFile = Paths.get(commonDirectory.absolutePath, "build.gradle")
        commonBuildFile.write(commonBuildFileContent)
        platformProject.settingsFile.append("include ':$commonProjectName'\n")
        return commonDirectory
    }

    static File createCommonSource(File commonDirectory,
                            Iterable<String> subdirectory,
                            String fileName,
                            String content) {
        def commonSrcDir = commonDirectory.toPath().resolve(Paths.get(*subdirectory))
        def commonSource = commonSrcDir.resolve(fileName)
        Files.createDirectories(commonSrcDir)
        commonSource.write(content)
        return commonSource.toFile()
    }

    def 'Plugin should support multiplatform projects'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    """\
                        @file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")
                        @OptionalExpectation
                        expect annotation class Optional()

                        @Optional
                        fun opt() = 42
                        
                        expect fun foo(): Int
                    """.stripIndent()
            )

            it.generateSrcFile("platform.kt", "actual fun foo() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                    }
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        project.createRunner().withArguments(":build").build()
    }

    def 'Multiplatform projects should be disabled by default'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun foo(): Int")

            it.generateSrcFile("platform.kt", "fun foo() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {}
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        project.createRunner().withArguments(":build").build()
    }

    def 'Plugin should use the \'main\' source set as a default common source set'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            Paths.get(commonDirectory.absolutePath, "build.gradle").append("""
                sourceSets {
                    common.kotlin.srcDir 'src/common/kotlin'
                }
                """.stripIndent())

            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun foo(): Int")
            createCommonSource(commonDirectory,
                    ["src", "common", "kotlin"],
                    "common.kt",
                    "expect fun bar(): Int")

            it.generateSrcFile("platform.kt", "actual fun foo() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                    }
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        project.createRunner().withArguments(":build").build()
    }

    def 'Plugin should allow a user to specify custom common source set'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            Paths.get(commonDirectory.absolutePath, "build.gradle").append("""
                sourceSets {
                    common.kotlin.srcDir 'src/common/kotlin'
                }
                """.stripIndent())

            createCommonSource(commonDirectory,
                    ["src", "common", "kotlin"],
                    "common.kt",
                    "expect fun bar(): Int")

            it.generateSrcFile("platform.kt", "actual fun bar() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                        commonSourceSets 'common'
                    }
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        project.createRunner().withArguments(":build").build()
    }

    def 'Plugin should allow setting several common source sets'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            Paths.get(commonDirectory.absolutePath, "build.gradle").append("""
                sourceSets {
                    common.kotlin.srcDir 'src/common/kotlin'
                }
                """.stripIndent())

            createCommonSource(commonDirectory,
                    ["src", "common", "kotlin"],
                    "common.kt",
                    "expect fun bar(): Int")

            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "main.kt",
                    "expect fun foo() : Int")

            it.generateSrcFile("platform.kt", "actual fun bar() = 42\nactual fun foo() = 43")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                        commonSourceSets 'common', 'main'
                    }
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        project.createRunner().withArguments(":build").build()
    }

    def 'Build should fail if the expectedBy dependency is not a project one'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun foo(): Int")

            it.generateSrcFile("platform.kt", "actual fun foo() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                    }
                }

                dependencies {
                    expectedBy files('common/src/main/kotlin/common.kt')
                }
                """.stripIndent())
        }
        def result = project.createRunner().withArguments(":build").buildAndFail()

        then:
        result.output.contains("dependency is not a project: ")
    }

    def 'Build should support several expectedBy-dependencies'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it, "commonFoo")
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun foo(): Int")

            commonDirectory = createCommonProject(it, "commonBar")
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun bar(): Int")

            it.generateSrcFile("platform.kt", "actual fun foo() = 0\nactual fun bar() = 0")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                    }
                }

                dependencies {
                    expectedBy project(':commonFoo')
                    expectedBy project(':commonBar')
                }
                """.stripIndent())
        }
        project.createRunner().withArguments(":build").build()
    }

    def 'Build should fail if the common project has no common plugin'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it,"common", "")
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun foo(): Int")

            it.generateSrcFile("platform.kt", "actual fun bar() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                    }
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        def result = project.createRunner().withArguments(":build").buildAndFail()

        then:
        result.output.contains("has an 'expectedBy' dependency to non-common project")
    }

    @Ignore("TODO in the Big Kotlin plugin")
    def 'Build should fail if custom common source set doesn\'t exist'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun foo(): Int")

            it.generateSrcFile("platform.kt", "actual fun bar() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                        commonSourceSets 'common'
                    }
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        def result = project.createRunner().withArguments(":build").buildAndFail()

        then:
        result.output.contains("Cannot find a source set with name 'common' in a common project")
    }

    def 'Setting custom source set should enable the multiplatform support'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            Paths.get(commonDirectory.absolutePath, "build.gradle").append("""
                sourceSets {
                    common.kotlin.srcDir 'src/common/kotlin'
                }
                """.stripIndent())

            createCommonSource(commonDirectory,
                    ["src", "common", "kotlin"],
                    "common.kt",
                    "expect fun bar(): Int")

            it.generateSrcFile("platform.kt", "actual fun bar() = 42")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        commonSourceSets 'common'
                    }
                }

                dependencies {
                    expectedBy project(':common')
                }
                """.stripIndent())
        }
        project.createRunner().withArguments(":build").build()
    }
}
