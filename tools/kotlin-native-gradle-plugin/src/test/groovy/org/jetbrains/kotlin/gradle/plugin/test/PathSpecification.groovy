package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.TargetManager

class PathSpecification extends BaseKonanSpecification {

    boolean fileExists(KonanProject project, String path) {
        project.konanBuildDir.toPath().resolve(path).toFile().exists()
    }

    def 'Plugin should create all necessary directories'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory)
        project.addCompilerArtifact("lib", "fun foo() {}", KonanProject.LIBRARY)
        project.addCompilerArtifact("bit", "fun bar() {}", KonanProject.BITCODE)
        println(project.buildFile.text)
        def result = project.createRunner().withArguments('build').build()

        then:
        project.konanBuildDir.toPath().resolve("bin/$KonanProject.HOST").toFile().listFiles().findAll {
            File it -> it.file && it.name.matches("^${KonanProject.DEFAULT_ARTIFACT_NAME}\\.[^.]+")
        }.size() > 0

        fileExists(project, "libs/$KonanProject.HOST/${KonanProject.DEFAULT_INTEROP_NAME}.klib")
        fileExists(project, "libs/$KonanProject.HOST/lib.klib")
        fileExists(project, "bitcode/$KonanProject.HOST/bit.bc")
    }

    def 'Plugin should stop building if the compiler classpath is empty'() {
        when:
        def project = KonanProject.create(projectDirectory)
        project.propertiesFile.write("konan.home=${projectDirectory.canonicalPath}}")
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.task(project.defaultCompilationTask()).outcome == TaskOutcome.FAILED
    }

    def 'Plugin should stop building if the stub generator classpath is empty'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory)
        project.propertiesFile.write("konan.home=${projectDirectory.canonicalPath}}")
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.task(project.compilationTask(KonanProject.DEFAULT_INTEROP_NAME)).outcome == TaskOutcome.FAILED
    }

    def 'Plugin should remove custom output directories'() {
        when:
        def customOutputDir = projectDirectory.toPath().resolve("foo").toFile()
        def project = KonanProject.create(projectDirectory) { KonanProject it ->
            it.addSetting("baseDir", customOutputDir)
        }

        def res1 = project.createRunner().withArguments("build").build()
        def artifactExistsAfterBuild = customOutputDir.toPath()
                .resolve("${KonanProject.HOST}").toFile()
                .listFiles()
                .findAll { it.name.matches("^${KonanProject.DEFAULT_ARTIFACT_NAME}\\.[^.]+") }.size() > 0

        def res2 = project.createRunner().withArguments("clean").build()
        def artifactDoesntNotExistAfterClean = customOutputDir.toPath()
                .resolve("${KonanProject.HOST}").toFile()
                .listFiles()
                .findAll { it.name.matches("^${KonanProject.DEFAULT_ARTIFACT_NAME}\\.[^.]+") }.isEmpty()

        then:
        res1.taskPaths(TaskOutcome.SUCCESS).containsAll(project.buildingTasks)
        res2.taskPaths(TaskOutcome.SUCCESS).contains(":clean")
        artifactExistsAfterBuild
        artifactDoesntNotExistAfterClean
    }
}
