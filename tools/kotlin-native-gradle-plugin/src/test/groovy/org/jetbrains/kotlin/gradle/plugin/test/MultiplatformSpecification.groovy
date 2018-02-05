package org.jetbrains.kotlin.gradle.plugin.test

import java.nio.file.Files
import java.nio.file.Paths

class MultiplatformSpecification extends BaseKonanSpecification {

    public static final String KOTLIN_VERSION = System.getProperty("kotlin.version")
    public static final String DEFAULT_COMMON_BUILD_FILE_CONTENT = """\
        buildscript {
            repositories {
                jcenter()
            }
            dependencies {
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION"
            }
        }

        apply plugin: 'kotlin-platform-common'
        
        repositories {
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
                    "expect fun foo(): Int")

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
                        commonSourceSet 'common'
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

    def 'Build should fail if several common projects are added'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = createCommonProject(it)
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "fun foo(): Int = 0")

            commonDirectory = createCommonProject(it, "common2")
            createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "fun bar(): Int = 0")

            it.generateSrcFile("platform.kt", "fun baz() = 0")
            it.buildFile.append("""
                konanArtifacts {
                    library('foo') {
                        enableMultiplatform true
                    }
                }

                dependencies {
                    expectedBy project(':common')
                    expectedBy project(':common2')
                }
                """.stripIndent())
        }
        def result = project.createRunner().withArguments(":build").buildAndFail()

        then:
        result.output.contains("has more than one 'expectedBy' dependency")
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
                        commonSourceSet 'common'
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
                        commonSourceSet 'common'
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
