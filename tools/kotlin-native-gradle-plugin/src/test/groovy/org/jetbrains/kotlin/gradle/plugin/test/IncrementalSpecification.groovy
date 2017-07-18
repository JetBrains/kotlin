package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

class IncrementalSpecification extends Specification {

    @Rule
    TemporaryFolder tmpFolder = new TemporaryFolder()

    Tuple buildTwice(KonanInteropProject project, Closure change) {
        def runner = project.createRunner().withArguments('build')
        def firstResult = runner.build()
        change(project)
        def secondResult = runner.build()
        return new Tuple(project, firstResult, secondResult)
    }

    Tuple buildTwice(Closure change) {
        return buildTwice(KonanInteropProject.create(tmpFolder), change)
    }

    Tuple buildTwiceEmpty(Closure change) {
        def project = KonanInteropProject.createEmpty(tmpFolder)
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
            project.buildFile.append("konanArtifacts['main'].$appending")
        }

        then:
        onlyRecompilationHappened(*results)


        where:
        parameter            | appending
        "outputDir"          | "outputDir 'build/new/outputDir'"
        "produce"            | "produce 'library'"
        "enableOptimization" | "enableOptimization()"
        "linkerOpts"         | "linkerOpts '--help'"
        "languageVersion"    | "languageVersion '1.2'"
        "apiVersion"         | "apiVersion '1.0'"
        "enableAssertions"   | "enableAssertions()"
        "enableDebug"        | "enableDebug true"
        "outputName"         | "outputName 'foo'"
    }

    def 'inputFiles change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.newFolder('src', 'foo', 'kotlin')
            it.generateSrcFile('src/foo/kotlin/', 'bar.kt', """
                fun main(args: Array<String>) { println("Hello!") }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            it.buildFile.append('konanArtifacts[\'main\'].inputFiles project.fileTree(\'src/foo/kotlin\')')
        }

        then:
        onlyRecompilationHappened(*results)
    }

    @Unroll("#parameter change for a compilation task should cause only recompilation")
    def 'Library changes should cause only recompilaton'() {
        when:
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.newFolder('src', 'lib', 'kotlin')
            it.generateSrcFile("src/lib/kotlin", "lib.kt", "fun bar() { println(\"Hello!\") }")
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
            it.buildFile.append("konanArtifacts['main'].$parameter konanArtifacts[\'lib\'].compilationTask.artifactPath")
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
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.buildFile.append("konanInterop { foo {} }\n")
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            it.buildFile.append('konanArtifacts[\'main\'].useInterop "foo"\n')
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'manifest parameter change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
        }
        def results = buildTwice(project) { KonanInteropProject it ->
            def manifest = it.generateSrcFile('manifest', "#some manifest file")
            it.buildFile.append("konanArtifacts['main'].manifest '${manifest.canonicalPath}'")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'manifest file change should cause only recompilation'() {
        when:
        def manifest
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            manifest = it.generateSrcFile('manifest', "#some manifest file\n")
            it.buildFile.append("konanArtifacts['main'].manifest '${manifest.canonicalPath}'")
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
            project.buildFile.append("konanInterop['stdio'].$appending")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)

        where:
        parameter            | appending
        "pkg"                | "pkg 'org.sample'"
        "compilerOpts"       | "compilerOpts '-g'"
        "linkerOpts"         | "linkerOpts '--help'"
        "includeDirs"        | "includeDirs 'src'"
    }

    def 'defFile change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(tmpFolder)
        project.generateSrcFile('main.kt')
        def defFile = project.generateDefFile("foo.def", "#some content")
        def results = buildTwice(project) { KonanInteropProject it ->
            it.buildFile.append("konanInterop['stdio'].defFile file('${defFile.canonicalPath}')\n")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'header change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(tmpFolder)
        project.generateSrcFile('main.kt')
        def header = project.generateSrcFile('header.h', "#define CONST 1")
        def results = buildTwice(project) { KonanInteropProject it ->
            it.buildFile.append("konanInterop['stdio'].headers '${header.canonicalPath}'\n")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'link change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.newFolder('src', 'lib', 'kotlin')
            it.generateSrcFile('src/lib/kotlin', 'lib.kt', 'fun foo() { println(42) }')
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
            it.buildFile.append("""
                konanInterop['stdio'].generateStubsTask.dependsOn(konanArtifacts['lib'].compilationTask)
                konanInterop['stdio'].link files(konanArtifacts['lib'].compilationTask.artifactPath)
            """.stripIndent())
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'konan version change should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
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

    @IgnoreIf({ System.getProperty('os.name').contains('windows') })
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
        def project = KonanInteropProject.createEmpty(tmpFolder) { KonanInteropProject it ->
            it.generateSrcFile('main.kt')
            it.propertiesFile.append("konan.build.targets=all\n")
        }


        def results = buildTwice(project) { KonanInteropProject it ->
            project.buildFile.append("konanArtifacts['main'].target '$newTarget'\n")
            project.buildFile.append("konanInterop['stdio'].target '$newTarget'\n")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    //endregion
}
