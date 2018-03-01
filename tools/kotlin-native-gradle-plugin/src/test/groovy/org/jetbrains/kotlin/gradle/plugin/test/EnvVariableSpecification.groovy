package org.jetbrains.kotlin.gradle.plugin.test

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import spock.lang.Unroll


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

        // Gradle TestKid doesn't support setting environment variables for runners.
        // So we use the following hack: we create a gradle wrapper, start it as a separate
        // process with custom environment variables and check its exit code.
        runner.withArguments("wrapper").build()

        def classpath = runner.pluginClasspath.collect { "'$it.absolutePath'" }.join(", ")
        project.buildFile.write("""\
                buildscript {
                    dependencies {
                        classpath files($classpath)
                    }
                }
                """.stripIndent())
        return project
    }

    private WrapperResult runWrapper(KonanProject project, List<String> tasks, Map<String, String> environment = [:]) {
        def wrapper = (HostManager.host.family == Family.WINDOWS) ? "gradlew.bat" : "gradlew"
        def command = ["$project.projectDir.absolutePath/$wrapper".toString()]
        command.addAll(tasks)
        def projectBuilder = new ProcessBuilder()
                .directory(project.projectDir)
                .command(command)
        projectBuilder.environment().putAll(environment)
        def process = projectBuilder.start()
        process.waitFor()
        return new WrapperResult(process)
    }

    private WrapperResult runWrapper(KonanProject project, String task, Map<String, String> environment = [:]) {
        return runWrapper(project, [task], environment)
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
        }
        return "$prefix${baseName}_${target.visibleName}.$suffix"
    }

    @Unroll("Plugin should support #action debug via an env variable")
    def 'Plugin should support enabling/disabling debug via an env variable'() {
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
                            $check
                        }
                    }
                }
                """.stripIndent())
        def result = runWrapper(project,"assertEnableDebug", ["DEBUGGING_SYMBOLS": value])
                .printStderr()
                .getExitValue()

        then:
        result == 0

        where:
        action      |value |check
        "enabling"  |"YES" |"if (!it.enableDebug) throw new AssertionError(\"Debug should be enabled for \${it.name}\")"
        "disabling" |"NO"  |"if (it.enableDebug)  throw new AssertionError(\"Debug should be disabled for \${it.name}\")"
    }

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
                            if (it.destinationDir.absolutePath != '$newDestinationPath'){
                                throw new AssertionError("Unexpected destination dir for \$it.name\\n" +
                                                         "expected: $newDestinationPath\\n" +
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
        if (HostManager.hostIsMac) {
            files.contains(artifactFileName("framework", ArtifactType.FRAMEWORK))
        }
    }

    def 'Plugin should throw an exception if CONFIGURATION_BUILD_DIR contains a relative path'() {
        when:
        def project = createProjectWithWrapper()
        project.buildFile.append("""\
            apply plugin: 'konan'
            konanArtifacts {
                library('main')
            }
        """.stripIndent())
        def wrapperResult = runWrapper(project, "tasks", ["CONFIGURATION_BUILD_DIR": "some_relative_path"])

        then:
        wrapperResult.getExitValue() != 0
        wrapperResult.getStderr().contains("A path passed using CONFIGURATION_BUILD_DIR should be absolute")
    }

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

}
