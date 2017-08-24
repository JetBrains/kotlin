package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

class IncrementalSpecification extends BaseKonanSpecification {

    Tuple buildTwice(KonanInteropProject project, Closure change) {
        def runner = project.createRunner().withArguments('build')
        def firstResult = runner.build()
        change(project)
        def secondResult = runner.build()
        return new Tuple(project, firstResult, secondResult)
    }

    Tuple buildTwice(Closure change) {
        return buildTwice(KonanInteropProject.create(projectDirectory), change)
    }

    Tuple buildTwiceEmpty(Closure change) {
        def project = KonanInteropProject.createEmpty(projectDirectory)
        project.generateSrcFile("main.kt")
        return buildTwice(project, change)
    }

    Boolean noRecompilationHappened(KonanInteropProject project, BuildResult firstResult, BuildResult secondResult) {
        return project.with {
            firstResult.tasks.collect { it.path }.containsAll(buildingTasks) &&
            firstResult.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks) &&
            secondResult.taskPaths(TaskOutcome.UP_TO_DATE).containsAll(buildingTasks) &&
            firstResult.task(downloadTask).outcome == TaskOutcome.SUCCESS &&
            secondResult.task(downloadTask).outcome == TaskOutcome.SUCCESS
        }
    }

    Boolean onlyRecompilationHappened(KonanInteropProject project, BuildResult firstResult, BuildResult secondResult) {
        return project.with {
            firstResult.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks) &&
            secondResult.taskPaths(TaskOutcome.SUCCESS).containsAll(compilationTasks) &&
            secondResult.taskPaths(TaskOutcome.UP_TO_DATE).containsAll(interopTasks)
        }
    }

    Boolean recompilationAndInteropProcessingHappened(KonanInteropProject project, BuildResult firstResult, BuildResult secondResult) {
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
        def results = buildTwice { KonanInteropProject project ->
            project.srcFiles[0].append("\n // Some change in the source file")
        }

        then:
        onlyRecompilationHappened(*results)

    }

    def 'Def-file change should cause recompilation and interop reprocessing'() {
        when:
        def results = buildTwice { KonanInteropProject project ->
            project.defFiles[0].append("\n # Some change in the def-file")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'Compilation is up-to-date if there is no changes in empty project'() {
        when:
        def results = buildTwiceEmpty {}

        then:
        noRecompilationHappened(*results)
    }

    @Unroll("#parameter change for a compilation task should cause only recompilation")
    def 'Parameter changes should cause only recompilaton'() {
        when:
        def results = buildTwiceEmpty { KonanInteropProject project ->
            project.addCompilationSetting("main", parameter, value)
        }

        then:
        onlyRecompilationHappened(*results)


        where:
        parameter            | value
        "outputDir"          | "'build/new/outputDir'"
        "produce"            | "'library'"
        "enableOptimization" | "()"
        "linkerOpts"         | "'--help'"
        "languageVersion"    | "'1.2'"
        "apiVersion"         | "'1.0'"
        "enableAssertions"   | "()"
        "enableDebug"        | "true"
        "outputName"         | "'foo'"
        "extraOpts"          | "'--time'"
    }

    def 'inputFiles change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.generateSrcFile(["src", "foo", "kotlin"], 'bar.kt', """
                fun main(args: Array<String>) { println("Hello!") }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            it.addCompilationSetting("main", "inputFiles", "project.fileTree('src/foo/kotlin')")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    @Unroll("#parameter change for a compilation task should cause only recompilation")
    def 'Library changes should cause only recompilaton'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.generateSrcFile(["src", "lib", "kotlin"], "lib.kt", "fun bar() { println(\"Hello!\") }")
            it.buildFile.append("""
                konanArtifacts {
                    lib {
                        inputFiles fileTree('src/lib/kotlin')
                        produce '$produce'
                    }
                }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            it.addCompilationSetting("main", parameter, "konanArtifacts['lib'].compilationTask.artifactPath")
        }

        then:
        onlyRecompilationHappened(*results)

        where:
        parameter       | produce
        "library"       | "library"
        "nativeLibrary" | "bitcode"
    }

    def 'useInterop change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.buildFile.append("konanInterop { foo {} }\n")
            it.generateDefFile("foo.def", "")
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            it.addCompilationSetting("main", "useInterop", "'foo'")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'manifest parameter change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            def manifest = it.generateSrcFile('manifest', "#some manifest file")
            it.addCompilationSetting("main", "manifest", manifest)
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'manifest file change should cause only recompilation'() {
        when:
        def manifest
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            manifest = it.generateSrcFile('manifest', "#some manifest file\n")
            it.addCompilationSetting("main", "manifest", manifest)
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            manifest.append("#something else\n")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    @Unroll("#parameter change for an interop task should cause recompilation and interop reprocessing")
    def 'Parameter change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def results = buildTwiceEmpty { KonanInteropProject project ->
            project.addInteropSetting("stdio", parameter, value)
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
    }

    def 'defFile change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory)
        project.generateSrcFile('main.kt')
        def defFile = project.generateDefFile("foo.def", "#some content")
        def results = buildTwice(project) { KonanInteropProject it ->
            it.addInteropSetting("stdio", "defFile", defFile)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'header change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory)
        project.generateSrcFile('main.kt')
        def header = project.generateSrcFile('header.h', "#define CONST 1")
        def results = buildTwice(project) { KonanInteropProject it ->
            it.addInteropSetting("stdio", "headers", header)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'link change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.generateSrcFile(["src", "lib", "kotlin"], 'lib.kt', 'fun foo() { println(42) }')
            it.buildFile.append("""
                konanArtifacts {
                    lib {
                        inputFiles fileTree('src/lib/kotlin')
                        produce 'bitcode'
                        noMain()
                    }
                }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            it.addInteropSetting("stdio", "generateStubsTask.dependsOn", "konanArtifacts['lib'].compilationTask")
            it.addInteropSetting("stdio", "link", "files(konanArtifacts['lib'].compilationTask.artifactPath)")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'konan version change should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.propertiesFile.append("konan.version=0.3\n")
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            def newText = it.propertiesFile.text.replace('konan.version=0.3', 'konan.version=0.4')
            it.propertiesFile.write(newText)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    @IgnoreIf({ System.getProperty('os.name').toLowerCase().contains('windows') })
    def 'target change should cause recompilation and interop reprocessing'() {
        when:
        def newTarget
        if (System.getProperty('os.name').toLowerCase().contains('linux')) {
            newTarget = "raspberrypi"
        } else if (System.getProperty('os.name').toLowerCase().contains('mac')) {
            newTarget = "iphone"
        } else {
            throw new IllegalStateException("Unknown host platform")
        }
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.propertiesFile.append("konan.build.targets=all\n")
        }


        def results = buildTwice(project) { KonanInteropProject it ->
            project.addCompilationSetting("main", "target", "'$newTarget'")
            project.addInteropSetting("stdio", "target", "'$newTarget'")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    //endregion
}
