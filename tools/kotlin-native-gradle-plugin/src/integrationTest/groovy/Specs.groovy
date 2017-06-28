import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class KonanProjectGenerator {
    static void generateBuildFile(File buildFile) {
        buildFile.write("""
            plugins { id 'konan' }
            
            konanInterop {
                stdio { }
            }
            
            konanArtifacts {
                main { useInterop 'stdio' }
            }
            """.stripIndent()
        )
    }

    static void generateSrcFile(File srcFile) {
        srcFile.write("""
            import kotlinx.cinterop.*
            import stdio.*
            
            fun main(args: Array<String>) {
                printf("%d\\n", 42)
            }
            """.stripIndent()
        )
    }

    static void generateDefFile(File defFile) {
        defFile.write("""
            headers = stdio.h stdlib.h string.h
            excludeDependentModules.osx = true
            """.stripIndent()
        )
    }

    static void generatePropertiesFile(File propertiesFile, String konanHome) {
        propertiesFile.write("konan.home=$konanHome")
    }
}

class IncrementalSpecification extends Specification {

    @Rule
    TemporaryFolder projectDir = new TemporaryFolder()

    File buildFile
    File srcFile
    File defFile
    File propertiesFile

    String konanHome = System.getProperty("konan.home") ?: { throw new IllegalStateException("konan.home isn't specified") }()
    String konanRoot = System.getProperty("konan.root") ?: { throw new IllegalStateException("konan.root isn't specified") }()

    def interopTasks = [":genStdioInteropStubs", ":compileStdioInteropStubs"].toSet()
    def compilationTasks = [":compileKonanMain", ":compileKonan", ":build"].toSet()

    def buildingTasks = interopTasks + compilationTasks
    def downloadTask = ":downloadKonanCompiler"
    def konanTasks = buildingTasks + downloadTask

    def setup() {
        buildFile = projectDir.newFile("build.gradle")
        projectDir.newFolder("src", "main", "kotlin")
        projectDir.newFolder("src", "main", "c_interop")
        srcFile = projectDir.newFile("src/main/kotlin/main.kt")
        defFile = projectDir.newFile("src/main/c_interop/stdio.def")
        propertiesFile = projectDir.newFile("gradle.properties")

        KonanProjectGenerator.generateBuildFile(buildFile)
        KonanProjectGenerator.generateDefFile(defFile)
        KonanProjectGenerator.generateSrcFile(srcFile)
        KonanProjectGenerator.generatePropertiesFile(propertiesFile, konanHome)
    }

    def 'Compilation is up-to-date if there is no changes'() {
        when:
        def runner = GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments('build')
                .withPluginClasspath()
        def result = runner.build()
        def upToDateResult = runner.build()

        then:
        result.tasks.collect { it.path }.containsAll(buildingTasks)
        result.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks)
        upToDateResult.taskPaths(TaskOutcome.UP_TO_DATE).containsAll(buildingTasks)
        result.task(downloadTask).outcome == TaskOutcome.SUCCESS
        upToDateResult.task(downloadTask).outcome == TaskOutcome.SUCCESS
    }

    def 'Source change should cause recompilation'() {
        when:
        def runner = GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments('build')
                .withPluginClasspath()
        def result = runner.build()
        srcFile.append("\n // Some change in the source file")
        def secondResult = runner.build()

        then:
        result.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks)
        secondResult.taskPaths(TaskOutcome.SUCCESS).containsAll(compilationTasks)
        secondResult.taskPaths(TaskOutcome.UP_TO_DATE).containsAll(interopTasks)
    }

    def 'Def-file change should cause recompilation'() {
        when:
        def runner = GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withArguments('build')
                .withPluginClasspath()
        def result = runner.build()
        defFile.append("\n # Some change in the def-file")
        def secondResult = runner.build()

        then:
        result.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks)
        secondResult.taskPaths(TaskOutcome.SUCCESS).containsAll(buildingTasks)
    }

    // TODO: Add checks for artifact/interop parameters.

}


