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

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import spock.lang.Ignore
import spock.lang.Unroll

import static org.jetbrains.kotlin.gradle.plugin.test.KonanProject.escapeBackSlashes

// TODO: Rewrite tests using Kotlin.

class EnvVariableSpecification extends BaseKonanSpecification {

    class WrapperResult {

        private int exitValue;
        private String stdout;
        private String stderr;

        WrapperResult(Process process) {
            exitValue = process.exitValue()
            stdout = process.getInputStream().readLines().join("\n")
            stderr = process.getErrorStream().readLines().join("\n")
        }

        int getExitValue() { return exitValue }
        String getStdout() { return stdout }
        String getStderr() { return stderr }

        WrapperResult printStdout() { println(stdout); return this }
        WrapperResult printStderr() { println(stderr); return this }
    }

    private KonanProject createProjectWithWrapper() {
        def project = KonanProject.createEmpty(projectDirectory)
        def runner = project.createRunner()

        // Gradle TestKit doesn't support setting environment variables for runners.
        // So we use the following hack: we create a gradle wrapper, start it as a separate
        // process with custom environment variables and check its exit code and output.
        runner.withArguments("wrapper").build()

        def classpath = runner.pluginClasspath.collect { "'${escapeBackSlashes(it.absolutePath)}'" }.join(", ")
        project.buildFile.write("""\
                buildscript {
                    dependencies {
                        classpath files($classpath)
                    }
                }
                """.stripIndent())
        return project
    }

    private WrapperResult runWrapper(KonanProject project,
                                     List<String> tasks,
                                     Map<String, String> environment = [:],
                                     Map<String, String> properties = ["konan.useEnvironmentVariables": 'true']) {
        def wrapper = (HostManager.host.family == Family.MINGW) ? "gradlew.bat" : "gradlew"
        def command = ["$project.projectDir.absolutePath/$wrapper".toString()]
        command.addAll(tasks)
        command.addAll(properties.collect { "-P${it.key}=${it.value}".toString() })
        def projectBuilder = new ProcessBuilder()
                .directory(project.projectDir)
                .command(command)
        projectBuilder.environment().putAll(environment)
        def process = projectBuilder.start()
        process.waitFor()
        return new WrapperResult(process)
    }

    private WrapperResult runWrapper(KonanProject project,
                                     String task,
                                     Map<String, String> environment = [:],
                                     Map<String, String> properties = ["konan.useEnvironmentVariables": 'true']) {
        return runWrapper(project, [task], environment, properties)
    }

    private String artifactFileName(String baseName, ArtifactType type, KonanTarget target = HostManager.host) {
        String suffix = ""
        String prefix = ""
        switch (type) {
            case ArtifactType.PROGRAM:
                suffix = target.family.exeSuffix
                break
            case ArtifactType.INTEROP:
            case ArtifactType.LIBRARY:
                suffix = "klib"
                break
            case ArtifactType.BITCODE:
                suffix = "bc"
                break;
            case ArtifactType.DYNAMIC:
                prefix = target.family.dynamicPrefix
                suffix = target.family.dynamicSuffix
                break
            case ArtifactType.STATIC:
                prefix = target.family.staticPrefix
                suffix = target.family.staticSuffix
                break
            case ArtifactType.FRAMEWORK:
                suffix = "framework"
        }
        return "$prefix${baseName}.$suffix"
    }

    @Ignore("The plugin doesn't use env vars until https://github.com/gradle/gradle/issues/3468 is fixed.")
    @Unroll("Plugin should support #action via an env variable")
    def 'Plugin should support enabling/disabling debug/opt via an env variable'() {
        when:
        def project = createProjectWithWrapper()
        project.buildFile.append("""\
                apply plugin: 'konan'
                konanArtifacts {
                    library('main')
                }

                task assertEnableDebug {
                    doLast {
                        konanArtifacts.main.forEach {
                            if (!($assertion)) throw new AssertionError("$message for \${it.name}")
                        }
                    }
                }
                """.stripIndent())
        def result = runWrapper(project,"assertEnableDebug", [(variable): value])
                .printStderr()
                .getExitValue()

        then:
        result == 0

        where:
        action            |variable                     |value |assertion                 |message
        "enabling debug"  |"DEBUGGING_SYMBOLS"          |"YES" |"it.enableDebug"          |"Debug should be enabled"
        "disabling debug" |"DEBUGGING_SYMBOLS"          |"NO"  |"!it.enableDebug"         |"Debug should be disabled"
        "enabling opt"    |"KONAN_ENABLE_OPTIMIZATIONS" |"YES" |"it.enableOptimizations"  |"Opts should be enabled"
        "disabling opt"   |"KONAN_ENABLE_OPTIMIZATIONS" |"NO"  |"!it.enableOptimizations" |"Opts should be disabled"
    }

    @Ignore("The plugin doesn't use env vars until https://github.com/gradle/gradle/issues/3468 is fixed.")
    def 'Plugin should support setting destination directory via an env variable'() {
        when:
        def project = createProjectWithWrapper()
        def newDestinationDir = project.createSubDir("newDestination")
        def newDestinationPath = newDestinationDir.absolutePath
        project.buildFile.append("""\
            apply plugin: 'konan'
            konanArtifacts {
                program('program')
                library('library')
                dynamic('dynamic')
                framework('framework')
            }

            task assertDestinationDir {
                doLast {
                    konanArtifacts.forEach { artifact ->
                        artifact.forEach {
                            if (it.destinationDir.absolutePath != '${escapeBackSlashes(newDestinationPath)}'){
                                throw new AssertionError("Unexpected destination dir for \$it.name\\n" +
                                                         "expected: ${escapeBackSlashes(newDestinationPath)}\\n" +
                                                         "actual: \$it.destinationDir")
                            }
                        }
                    }
                }
            }
        """.stripIndent())
        project.generateSrcFile("main.kt")
        def assertResult = runWrapper(project, "assertDestinationDir", ["CONFIGURATION_BUILD_DIR": newDestinationPath])
                .printStderr()
                .getExitValue()
        def buildResult = runWrapper(project, "build", ["CONFIGURATION_BUILD_DIR": newDestinationPath])
                .printStderr()
                .getExitValue()
        def files = newDestinationDir.list()

        then:
        assertResult == 0
        buildResult == 0
        files.contains(artifactFileName("program", ArtifactType.PROGRAM))
        files.contains(artifactFileName("library", ArtifactType.LIBRARY))
        files.contains(artifactFileName("dynamic", ArtifactType.DYNAMIC))
        files.contains(artifactFileName("static", ArtifactType.STATIC))
        if (HostManager.hostIsMac) {
            files.contains(artifactFileName("framework", ArtifactType.FRAMEWORK))
        }
    }

    @Ignore("The plugin doesn't use env vars until https://github.com/gradle/gradle/issues/3468 is fixed.")
    def 'Plugin should rerun tasks if CONFIGURATION_BUILD_DIR has been changed'() {
        when:
        def project = createProjectWithWrapper()
        def destination1 = project.createSubDir("destination1", "subdir")
        def destination2 = project.createSubDir("destination2", "subdir")
        project.buildFile.append("""\
            apply plugin: 'konan'
            konanArtifacts {
                library('main')
            }
        """.stripIndent())
        project.generateSrcFile("main.kt")

        def buildResult1 = runWrapper(project, "build", ["CONFIGURATION_BUILD_DIR": destination1.absolutePath])
                .printStderr()
                .getExitValue()
        def buildResult2 = runWrapper(project, "build", ["CONFIGURATION_BUILD_DIR": destination2.absolutePath])
                .printStderr()
                .getExitValue()
        def files1 = destination1.list()
        def files2 = destination2.list()

        then:
        buildResult1 == 0
        buildResult2 == 0
        destination1.exists()
        destination2.exists()
        files1.contains(artifactFileName("main", ArtifactType.LIBRARY))
        files2.contains(artifactFileName("main", ArtifactType.LIBRARY))
    }

    @Ignore("The plugin doesn't use env vars until https://github.com/gradle/gradle/issues/3468 is fixed.")
    def 'Plugin should ignore environmentVariables if konan.useEnvironmentVariables is false or is not set'() {
        when:
        def project = createProjectWithWrapper()
        def newDestinationDir = project.createSubDir("newDestination")
        def newDestinationPath = newDestinationDir.absolutePath
        project.buildFile.append("""\
            apply plugin: 'konan'
            konanArtifacts {
                program('program')
                library('library')
                dynamic('dynamic')
                framework('framework')
            }

            task assertNoOverrides {
                doLast {
                    konanArtifacts.forEach { artifact ->
                        artifact.forEach {
                            if (it.destinationDir.absolutePath == '${escapeBackSlashes(newDestinationPath)}'){
                                throw new AssertionError("CONFIGURATION_BUILD_DIR overrides a default output path " +
                                                         "when it shouldn't.\\n" +
                                                         "Task: \${it.name}, Path: \${it.destinationDir}")
                            }
                            
                            if (it.enableDebug) {
                                throw new AssertionError("DEBUGGING_SYMBOLS overrides a default value " +
                                                         "when it shouldn't\\n" +
                                                         "Task: \${it.name}")
                            }
                        }
                    }
                }
            }
            """.stripIndent())
        def resultNoProp = runWrapper(project,
                "assertNoOverrides",
                ["DEBUGGING_SYMBOLS": "true", "CONFIGURATION_BUILD_DIR": newDestinationPath], [:])
                .printStderr()
                .getExitValue()
        def resultFalseValue = runWrapper(project,
                "assertNoOverrides",
                ["DEBUGGING_SYMBOLS": "true", "CONFIGURATION_BUILD_DIR": newDestinationPath],
                ["konan.useEnvironmentVariables": "false"])
                .printStderr()
                .getExitValue()

        then:
        resultNoProp == 0
        resultFalseValue == 0
    }

    @Ignore("The plugin doesn't use env vars until https://github.com/gradle/gradle/issues/3468 is fixed.")
    def 'Up-to-date checks should work with different directories for different targets'() {
        when:
        def project = createProjectWithWrapper()
        def fooDir = project.createSubDir("foo")
        def barDir = project.createSubDir("bar")
        project.buildFile.append("""\
            apply plugin: 'konan'
            
            konanArtifacts {
                library('foo')
                library('bar')
            }
            
            task assertUpToDate {
                dependsOn 'compileKonanFoo'
                doLast {
                    if (!konanArtifacts.foo.getByTarget('host').state.upToDate) {
                        throw new AssertionError("Compilation task is not up-to-date")
                    }
                }
            }
            """.stripIndent())
        project.generateSrcFile("main.kt")

        def buildResult1 = runWrapper(project, "compileKonanFoo",
                ["CONFIGURATION_BUILD_DIR": fooDir.absolutePath])
                .printStderr()
                .getExitValue()
        def buildResult2 = runWrapper(project, "compileKonanBar",
                ["CONFIGURATION_BUILD_DIR": barDir.absolutePath])
                .printStderr()
                .getExitValue()
        def buildResult3 = runWrapper(project,
                "assertUpToDate",
                ["CONFIGURATION_BUILD_DIR": fooDir.absolutePath])
                .printStderr()
                .getExitValue()

        then:
        buildResult1 == 0
        buildResult2 == 0
        buildResult3 == 0
    }

}
