package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Paths

class TaskSpecification extends BaseKonanSpecification {

    def 'Configs should allow user to add dependencies to them'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile("main.kt")
        }
        project.buildFile.append("""
            task beforeInterop(type: DefaultTask) { doLast { println("Before Interop") } }
            task beforeCompilation(type: DefaultTask) { doLast { println("Before compilation") } }
        """.stripIndent())
        project.addInteropSetting("dependsOn", "beforeInterop")
        project.addCompilationSetting("dependsOn", "beforeCompilation")
        def result = project.createRunner().withArguments('build').build()

        then:
        def beforeInterop = result.task(":beforeInterop")
        beforeInterop != null && beforeInterop.outcome == TaskOutcome.SUCCESS
        def beforeCompilation = result.task(":beforeCompilation")
        beforeCompilation != null && beforeCompilation.outcome == TaskOutcome.SUCCESS
    }

    def 'Compilation config should work with konanInterop from another project'() {
        when:
        def rootProject = KonanProject.create(projectDirectory) { KonanProject it ->
            it.buildFile.append("evaluationDependsOn(':interop')\n")
            it.createFile("settings.gradle", "include ':interop'")
            it.addCompilationSetting("useInterop", "project(':interop').konanInterop['interop']")
        }
        def interopProjectDir = rootProject.createSubDir("interop")
        def interopProject = KonanInteropProject.createEmpty(interopProjectDir) { KonanInteropProject it ->
            it.generateBuildFile("""
                apply plugin: 'konan'

                konanInterop {
                    interop { }
                }
            """.stripIndent())
            it.interopTasks = [":interop:processInteropInterop"]
            it.generateDefFile("interop.def")
        }
        def result = rootProject.createRunner().withArguments("build").build()

        then:
        result.taskPaths(TaskOutcome.SUCCESS).containsAll(rootProject.compilationTasks + interopProject.interopTasks)
    }

    def 'Compilation should support interop parameters changing after `useInterop` call'() {
        when:
        def project = KonanInteropProject.create(projectDirectory)
        project.addInteropSetting("linkerOpts", "'-lpthread'")
        project.buildFile.append("""
            task printArgs {
                dependsOn 'build'
                doLast {
                    println(konanArtifacts['$project.DEFAULT_ARTIFACT_NAME'].compilationTask.linkerOpts)
                    konanArtifacts['$project.DEFAULT_ARTIFACT_NAME'].compilationTask.libraries.each { println it.files }
                    konanArtifacts['$project.DEFAULT_ARTIFACT_NAME'].compilationTask.nativeLibraries.each { println it.files }
                }
            }
        """.stripIndent())
        def result = project.createRunner().withArguments('printArgs').build()

        then:
        result.task(":printArgs") != null
        result.task(":printArgs").outcome == TaskOutcome.SUCCESS
        def expectedKlibPath = project.konanBuildDir.toPath()
                .resolve("c_interop${File.separator}stdio.klib")
                .toFile().canonicalPath
        def ls = System.lineSeparator()
        result.output.contains("[-lpthread]$ls[$expectedKlibPath]".stripIndent().trim())
    }

    def 'Compiler should print time measurements if measureTime flag is set'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory)
        project.generateSrcFile("main.kt")
        project.addCompilationSetting("measureTime", "true")
        def result = project.createRunner().withArguments('build').build()

        then:
        result.output.findAll(~/FRONTEND:\s+\d+\s+msec/).size() == 1
        result.output.findAll(~/BACKEND:\s+\d+\s+msec/).size() == 1
        result.output.findAll(~/LINK_STAGE:\s+\d+\s+msec/).size() == 1
    }

    BuildResult failOnPropertyAccess(String property) {
        def project = KonanInteropProject.createEmpty(projectDirectory)
         project.buildFile.append("""
            task testTask(type: DefaultTask) {
                doLast {
                    println(${project.defaultInteropConfig()}.$property)
                }
            }
        """.stripIndent())
        return project.createRunner().withArguments("testTask").buildAndFail()
    }

    BuildResult failOnTaskAccess(String task) {
        def project = KonanInteropProject.createEmpty(projectDirectory)
        project.buildFile.append("""
            task testTask(type: DefaultTask) {
                dependsOn $task
            }
        """.stripIndent())
        return project.createRunner().withArguments("testTask").buildAndFail()
    }

    def 'Deprecated properties/tasks should generate exceptions'() {
        expect:
        failOnPropertyAccess("generateStubsTask")
        failOnPropertyAccess("compileStubsTask")
        failOnPropertyAccess("compileStubsConfig")
        failOnTaskAccess("gen${KonanInteropProject.DEFAULT_INTEROP_NAME.capitalize()}InteropStubs")
        failOnTaskAccess("compile${KonanInteropProject.DEFAULT_INTEROP_NAME.capitalize()}InteropStubs")
    }

    def 'Compilation task should be able to use another project as a library (with artifact name)'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject prj ->
            prj.generateSrcFile("main.kt", """\
                external fun foo()
                fun main(args: Array<String>) = foo()
            """)
            prj.settingsFile.append("include ':foo'")
        }
        def subproject = KonanProject.createEmpty(project.projectPath.resolve("foo").toFile()) {
            KonanProject prj ->
            prj.generateSrcFile("foo.kt", "fun foo() { println(42) } ")
            prj.addCompilationSetting("produce", "'library'")
            def buildScript = prj.buildFile.text.replace("plugins { id 'konan' }", "apply plugin: 'konan'")
            prj.buildFile.write(buildScript)
        }
        project.addCompilationSetting("library", "findProject(':foo'), 'main'")

        def result = project.createRunner().withArguments("build").build()
        def successfulTasks = project.buildingTasks + subproject.buildingTasks.collect { ":foo$it".toString() }

        subproject.addCompilationSetting("produce", "'program'")
        def failedResult = project.createRunner().withArguments("build").buildAndFail()


        then:
        result.taskPaths(TaskOutcome.SUCCESS).containsAll(successfulTasks)
    }

    def 'Compilation task should be able to use another project as a library (all klibs in the project)'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject prj ->
            prj.generateSrcFile("main.kt", """\
                external fun foo()
                external fun bar()
                fun baz() { println("baz") }
                fun main(args: Array<String>) { foo(); bar(); baz() }
            """.stripIndent())
            prj.settingsFile.append("include ':foo'")
        }
        def subproject = KonanProject.createEmpty(project.projectPath.resolve("foo").toFile()) {
            KonanProject prj ->
                def buildScript = prj.buildFile.text.replace("plugins { id 'konan' }", "apply plugin: 'konan'")
                prj.buildFile.write(buildScript)

                // Default artifact: main, should be recognized as a library with foo function.
                prj.generateSrcFile("foo.kt", "fun foo() { println(42) } ")
                prj.addCompilationSetting("produce", "'library'")

                prj.generateSrcFile(Paths.get("src", "bar" , "kotlin"),
                        "bar.kt", "fun bar() { println(\"bar\") }")

                prj.generateSrcFile(Paths.get("src", "baz", "kotlin"),
                        "baz.kt", "fun baz() { println(\"another baz\") }")

                prj.buildFile.append("""\
                    konanArtifacts {
                        // Should be recognized as a library.
                        bar {
                            inputDir "src/bar/kotlin"
                            produce "library"
                        }
                    
                        // Should not be recognized as a library.
                        baz {
                            inputDir "src/baz/kotlin"
                            produce "bitcode"
                        }
                    }
                """.stripIndent())

        }
        project.addCompilationSetting("library", "findProject(':foo')")

        def result = project.createRunner().withArguments("build").build()
        def successfulTasks = project.buildingTasks + subproject.buildingTasks.collect { ":foo$it".toString() }

        then:
        result.taskPaths(TaskOutcome.SUCCESS).containsAll(successfulTasks)
    }
}
