package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder

class KonanProject {

    TemporaryFolder projectDir

    File getProjectDirRoot() { return projectDir.root }

    String konanHome = System.getProperty("konan.home") ?: { throw new IllegalStateException("konan.home isn't specified") }()

    File         buildFile
    File         propertiesFile

    Set<File>   srcFiles = []

    List<String> compilationTasks = []
    String       downloadTask = ":downloadKonanCompiler"

    List<String> getBuildingTasks() { return compilationTasks }
    List<String> getKonanTasks()    { return getBuildingTasks() + downloadTask }

    GradleRunner createRunner() { return GradleRunner.create().withProjectDir(projectDirRoot).withPluginClasspath() }

    protected KonanProject(TemporaryFolder projectDir) {
        this.projectDir = projectDir
    }

    void generateFolders() {
        projectDir.newFolder("src", "main", "kotlin")
    }

    File generateBuildFile(String content) {
        buildFile = projectDir.newFile("build.gradle")
        buildFile.write(content)
        return buildFile
    }

    File generateBuildFile() {
        def result = generateBuildFile("""
            plugins { id 'konan' }
            
            konanArtifacts {
                main { }
            }
            """.stripIndent()
        )
        compilationTasks = [":compileKonanMain", ":compileKonan", ":build"]
        return result
    }

    File generateSrcFile(String directoryPath, String fileName, String content) {
        def result = projectDir.newFile("$directoryPath/$fileName")
        result.write(content)
        srcFiles.add(result)
        return result
    }

    File generateSrcFile(String fileName, String content) {
        return generateSrcFile("src/main/kotlin",fileName, content)
    }

    File generateSrcFile(String fileName) {
        return generateSrcFile(fileName, """           
            fun main(args: Array<String>) {
                println(42)
            }
            """.stripIndent()
        )
    }

    File generatePropertiesFile(String konanHome) {
        propertiesFile = projectDir.newFile("gradle.properties")
        propertiesFile.write("konan.home=$konanHome\n")
        return propertiesFile
    }

    File newFolder(String... path) { return projectDir.newFolder(path) }

    static KonanProject create(TemporaryFolder projectDir) {
        return createEmpty(projectDir) {
            it.generateSrcFile("main.kt")
        }
    }

    static KonanProject create(TemporaryFolder projectDir, Closure config) {
        def result = create(projectDir)
        config(result)
        return result
    }

    static KonanProject createEmpty(TemporaryFolder projectDir) {
        def result = new KonanProject(projectDir)
        result.with {
            generateFolders()
            generateBuildFile()
            generatePropertiesFile(konanHome)
        }
        return result
    }

    static KonanProject createEmpty(TemporaryFolder projectDir, Closure config) {
        def result = createEmpty(projectDir)
        config(result)
        return result
    }

}

class KonanInteropProject extends KonanProject {

    Set<File> defFiles = []

    List<String> interopTasks = []

    protected KonanInteropProject(TemporaryFolder projectDir) { super(projectDir) }

    List<String> getBuildingTasks() { return interopTasks + compilationTasks }

    void generateFolders() {
        super.generateFolders()
        projectDir.newFolder("src", "main", "c_interop")
    }

    File generateBuildFile() {
        def result = generateBuildFile("""
            plugins { id 'konan' }
            
            konanInterop {
                stdio { }
            }
            
            konanArtifacts {
                main { useInterop 'stdio' }
            }
            """.stripIndent()
        )
        interopTasks = [":genStdioInteropStubs", ":compileStdioInteropStubs"]
        compilationTasks = [":compileKonanMain", ":compileKonan", ":build"]
        return result
    }

    File generateDefFile(String fileName, String content) {
        def result = projectDir.newFile("src/main/c_interop/$fileName")
        result.write(content)
        defFiles.add(result)
        return result
    }

    File generateDefFile(String fileName) {
        return generateDefFile(fileName, """
            headers = stdio.h stdlib.h string.h
            excludeDependentModules.osx = true
            """.stripIndent()
        )
    }

    static KonanInteropProject create(TemporaryFolder projectDir) {
        return createEmpty(projectDir) {
            it.generateSrcFile("main.kt")
            it.generateDefFile("stdio.def")
        }
    }

    static KonanInteropProject create(TemporaryFolder projectDir, Closure config) {
        def result = create(projectDir)
        config(result)
        return result
    }

    static KonanInteropProject createEmpty(TemporaryFolder projectDir) {
        def result = new KonanInteropProject(projectDir)
        result.with {
            generateFolders()
            generateBuildFile()
            generatePropertiesFile(konanHome)
        }
        return result
    }

    static KonanInteropProject createEmpty(TemporaryFolder projectDir, Closure config) {
        def result = createEmpty(projectDir)
        config(result)
        return result
    }
}