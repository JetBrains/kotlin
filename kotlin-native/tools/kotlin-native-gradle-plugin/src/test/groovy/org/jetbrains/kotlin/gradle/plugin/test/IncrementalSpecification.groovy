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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

class IncrementalSpecification extends BaseKonanSpecification {

    Tuple buildTwice(KonanProject project, String task = 'build', Closure change) {
        def runner = project.createRunner().withArguments(task)
        def firstResult = runner.build()
        change(project)
        def secondResult = runner.build()
        return new Tuple(project, firstResult, secondResult)
    }

    Tuple buildTwice(ArtifactType mainArtifactType = ArtifactType.LIBRARY, String task = 'build', Closure change) {
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
        "extraOpts"           | "'-Xtime'"
        "noDefaultLibs"       | "true"
        "noEndorsedLibs"       | "true"
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
                fun foo(args: Array<String>) { println("Hello!") }
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
        "extraOpts"              | "'-verbose'"
        "noDefaultLibs"          | "true"
        "noEndorsedLibs"          | "true"
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

    def 'Common source change should cause recompilation'() {
        when:
        File commonSource
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            def commonDirectory = MultiplatformSpecification.createCommonProject(it)
            commonSource = MultiplatformSpecification.createCommonSource(commonDirectory,
                    ["src", "main", "kotlin"],
                    "common.kt",
                    "expect fun foo(): Int")
            println(it.settingsFile.text)

            it.generateSrcFile("actual.kt", "actual fun foo() = 42")
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
            it.buildingTasks.addAll([":compileKonanFoo", ":compileKonanFoo${KonanProject.HOST}}"])
        }

        def results = buildTwice(project, ':build') { KonanProject ->
            commonSource.append("\nfun bar() = 43")
        }

        then:
        onlyRecompilationHappened(*results)
    }

    // TODO: Add incremental tests for the 'libraries' block.

    //endregion
}
