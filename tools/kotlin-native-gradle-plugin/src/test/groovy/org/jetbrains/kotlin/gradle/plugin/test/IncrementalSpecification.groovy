package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

class IncrementalSpecification extends BaseKonanSpecification {

    Tuple buildTwice(KonanProject project, Closure change) {
        def runner = project.createRunner().withArguments('build')
        def firstResult = runner.build()
        change(project)
        def secondResult = runner.build()
        return new Tuple(project, firstResult, secondResult)
    }

    Tuple buildTwice(ArtifactType mainArtifactType = ArtifactType.LIBRARY, Closure change) {
        return buildTwice(KonanProject.createWithInterop(projectDirectory, mainArtifactType), change)
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
        "enableAssertions"    | "true"
        "enableDebug"         | "true"
        "artifactName"        | "'foo'"
        "extraOpts"           | "'--time'"
        "noDefaultLibs"       | "true"
    }

    def 'Plugin should support a custom entry point and recompile an artifact if it changes'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("main", """
                |fun main(args: Array<String>) { println("default main") }
                |
            """.stripMargin())
        }
        def results = buildTwice(project) { KonanProject it ->
            it.srcFiles[0].write("""
                |package foo
                |
                |fun bar(args: Array<String>) { println("changed main") }
                |
            """.stripMargin())
            it.addSetting("main", "entryPoint", "'foo.bar'")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'srcFiles change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY) { KonanProject it ->
            it.generateSrcFile(["src", "foo", "kotlin"], 'bar.kt', """
                fun main(args: Array<String>) { println("Hello!") }
            """.stripIndent())
        }
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("main", "srcFiles", "project.fileTree('src/foo/kotlin')")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    def 'Library change for a compilation task should cause only recompilation'() {
        when:
        def project = KonanProject.create(projectDirectory, ArtifactType.LIBRARY) { KonanProject it ->
            it.generateSrcFile(["src", "lib", "kotlin"], "lib.kt", "fun bar() { println(\"Hello!\") }")
            it.buildFile.append("""
                konanArtifacts {
                    library('lib') {
                        srcFiles fileTree('src/lib/kotlin')
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

    def 'Native library change for a compilation task should cause only recompilaton'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY) { KonanProject it ->
            it.generateSrcFile(["src", "lib", "kotlin"], "lib.kt", "fun bar() { println(\"Hello!\") }")
            it.buildFile.append("""
                konanArtifacts {
                    bitcode('lib') {
                        srcFiles fileTree('src/lib/kotlin')
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
        parameter                | value
        "packageName"            | "'org.sample'"
        "compilerOpts"           | "'-g'"
        "linkerOpts"             | "'--help'"
        "includeDirs"            | "'src'"
        "includeDirs.allHeaders" | "'src'"
        "extraOpts"              | "'-shims', 'false'"
        "noDefaultLibs"          | "true"
    }

    def 'includeDirs.headerFilterOnly change should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory) { KonanProject it ->
            it.defFiles.first().write("headers = stdio.h\nheaderFilter = stdio.h")
        }
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting(KonanProject.DEFAULT_INTEROP_NAME, "includeDirs.headerFilterOnly", "'.'")
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'defFile change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY)
        def defFile = project.generateDefFile("foo.def", "#some content")
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("stdio", "defFile", defFile)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'header change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY)
        def header = project.generateSrcFile('header.h', "#define CONST 1")
        def results = buildTwice(project) { KonanProject it ->
            it.addSetting("stdio", "headers", header)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    def 'link change for an interop task should cause recompilation and interop reprocessing'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY) { KonanProject it ->
            it.generateSrcFile(["src", "lib", "kotlin"], 'lib.kt', 'fun foo() { println(42) }')
            it.buildFile.append("""
                konanArtifacts {
                    bitcode('lib') {
                        srcFiles fileTree('src/lib/kotlin')
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
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY) { KonanProject it ->
            it.propertiesFile.append("konan.version=0.3\n")
        }
        def results = buildTwice(project) { KonanProject it ->
            def newText = it.propertiesFile.text.replace('konan.version=0.3', 'konan.version=0.4')
            it.propertiesFile.write(newText)
        }

        then:
        recompilationAndInteropProcessingHappened(*results)
    }

    // TODO: Add incremental tests for the 'libraries' block.

    //endregion
}
