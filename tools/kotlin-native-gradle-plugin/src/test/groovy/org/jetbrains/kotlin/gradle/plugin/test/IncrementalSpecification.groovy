package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf
import spock.lang.Unroll

class IncrementalSpecification extends BaseKonanSpecification {

    Tuple buildTwice(KonanProject project, Closure change) {
        def runner = project.createRunner().withArguments('build')
        def firstResult = runner.build()
        change(project)
        def secondResult = runner.build()
        return new Tuple(project, firstResult, secondResult)
    }

    Tuple buildTwice(Closure change) {
        return buildTwice(KonanProject.createWithInterop(projectDirectory), change)
    }

    Boolean noRecompilationHappened(KonanProject project, BuildResult firstResult, BuildResult secondResult) {
        return project.with {
            firstResult.tasks.collect { it.path }.containsAll(buildingTasks) &&
            firstResult.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks) &&
            secondResult.taskPaths(TaskOutcome.UP_TO_DATE).containsAll(buildingTasks) &&
            firstResult.task(downloadTask).outcome == TaskOutcome.SUCCESS &&
            secondResult.task(downloadTask).outcome == TaskOutcome.SUCCESS
        }
    }

    Boolean onlyRecompilationHappened(KonanProject project, BuildResult firstResult, BuildResult secondResult) {
        return project.with {
            firstResult.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks) &&
            secondResult.taskPaths(TaskOutcome.SUCCESS).containsAll(compilationTasks) &&
            secondResult.taskPaths(TaskOutcome.UP_TO_DATE).containsAll(interopTasks)
        }
    }

    Boolean recompilationAndInteropProcessingHappened(KonanProject project, BuildResult firstResult, BuildResult secondResult) {
        return project.with {
            firstResult.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks) &&
            secondResult.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks)
        }
    }

    //region tests =====================================================================================================
    def 'Compilation is up-to-date if there is no changes'() {
        when:
        def results = buildTwice {}

        then:
        noRecompilationHappened(*results)
    }

    def 'Source change should cause only recompilation'() {
        when:
        def results = buildTwice { KonanProject project ->
            project.srcFiles[0].append("\n // Some change in the source file")
        }

        then:
        onlyRecompilationHappened(*results)

    }

    def 'Def-file change should cause recompilation and interop reprocessing'() {
        when:
        def results = buildTwice { KonanProject project ->
            project.defFiles[0].append("\n # Some change in the def-file")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'Compilation is up-to-date if there is no changes in empty project'() {
        when:
        def results = buildTwice {}

        then:
        noRecompilationHappened(*results)
    }

    @Unroll("#parameter change for a compilation task should cause only recompilation")
    def 'Parameter changes should cause only recompilaton'() {
        when:
        def results = buildTwice { KonanProject project ->
            project.addSetting("main", parameter, value)
        }

        then:
        onlyRecompilationHappened(*results)


        where:
        parameter             | value
        "baseDir"             | "'build/new/outputDir'"
        "enableOptimizations" | "true"
        "linkerOpts"          | "'--help'"
        "languageVersion"     | "'1.2'"
        "apiVersion"          | "'1.0'"
        "enableAssertions"    | "true"
        "enableDebug"         | "true"
        "outputName"          | "'foo'"
        "extraOpts"           | "'--time'"
        "noDefaultLibs"       | "true"
    }

    def 'inputFiles change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory) { KonanProject it ->
            it.generateSrcFile(["src", "foo", "kotlin"], 'bar.kt', """
                fun main(args: Array<String>) { println("Hello!") }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("main", "inputFiles", "project.fileTree('src/foo/kotlin')")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'Library change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanProject.create(projectDirectory) { KonanProject it ->
            it.generateSrcFile(["src", "lib", "kotlin"], "lib.kt", "fun bar() { println(\"Hello!\") }")
            it.buildFile.append("""
                konanArtifacts {
                    library('lib') {
                        inputFiles fileTree('src/lib/kotlin')
                    }
                }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanProject it ->
            it.addLibraryToArtifact("main", 'lib')
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'Library changes should cause only recompilaton'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory) { KonanProject it ->
            it.generateSrcFile(["src", "lib", "kotlin"], "lib.kt", "fun bar() { println(\"Hello!\") }")
            it.buildFile.append("""
                konanArtifacts {
                    bitcode('lib') {
                        inputFiles fileTree('src/lib/kotlin')
                    }
                }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("main", "nativeLibrary", "compileKonanLib${KonanProject.HOST.capitalize()}.artifact")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    // TODO: Test library for incremental compilation.

    @Unroll("#parameter change for an interop task should cause recompilation and interop reprocessing")
    def 'Parameter change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def results = buildTwice { KonanProject project ->
            project.addSetting("stdio", parameter, value)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)

        where:
        parameter            | value
        "pkg"                | "'org.sample'"
        "compilerOpts"       | "'-g'"
        "linkerOpts"         | "'--help'"
        "includeDirs"        | "'src'"
        "extraOpts"          | "'-shims', 'false'"
        "noDefaultLibs"      | "true"
    }

    def 'defFile change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory)
        def defFile = project.generateDefFile("foo.def", "#some content")
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("stdio", "defFile", defFile)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'header change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory)
        def header = project.generateSrcFile('header.h', "#define CONST 1")
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("stdio", "headers", header)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'link change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory) { KonanProject it ->
            it.generateSrcFile(["src", "lib", "kotlin"], 'lib.kt', 'fun foo() { println(42) }')
            it.buildFile.append("""
                konanArtifacts {
                    bitcode('lib') {
                        inputFiles fileTree('src/lib/kotlin')
                    }
                }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("stdio", "dependsOn", "konanArtifacts.lib.${KonanProject.HOST}")
            it.addSetting("stdio", "link", "files(konanArtifacts.lib.${KonanProject.HOST}.artifactPath)")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'konan version change should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory) { KonanProject it ->
            it.propertiesFile.append("konan.version=0.3\n")
        }
        def results = buildTwice(project) { KonanProject it ->
            def newText = it.propertiesFile.text.replace('konan.version=0.3', 'konan.version=0.4')
            it.propertiesFile.write(newText)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    //endregion
}
