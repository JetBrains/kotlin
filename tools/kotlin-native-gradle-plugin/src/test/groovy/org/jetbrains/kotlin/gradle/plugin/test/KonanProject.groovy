package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder

class KonanProject {

    static String DEFAULT_ARTIFACT_NAME = 'main'

    TemporaryFolder projectDir

    File getProjectDirRoot() { return projectDir.root }

    String konanHome

    File         buildFile
    File         propertiesFile

    Set<File>   srcFiles = []

    List<String> compilationTasks = []
    String       downloadTask = ":downloadKonanCompiler"

    List<String> getBuildingTasks() { return compilationTasks }
    List<String> getKonanTasks()    { return getBuildingTasks() + downloadTask }

    GradleRunner createRunner(boolean withDebug = true) {
        return GradleRunner.create().withProjectDir(projectDirRoot).withPluginClasspath().withDebug(withDebug)
    }

    protected KonanProject(TemporaryFolder projectDir) {
        this.projectDir = projectDir
        def konanHome = System.getProperty("konan.home")
        if (konanHome == null) {
            throw new IllegalStateException("konan.home isn't specified")
        }
        def konanHomeDir = new File(konanHome)
        if (!konanHomeDir.exists() || !konanHomeDir.directory) {
            throw new IllegalStateException("konan.home doesn't exist or is not a directory: $konanHomeDir.canonicalPath")
        }
        // Escape windows path separator
        this.konanHome = konanHomeDir.canonicalPath.replace('\\', '\\\\')
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
                $DEFAULT_ARTIFACT_NAME { }
            }
            """.stripIndent()
        )
        compilationTasks = [":compileKonan${DEFAULT_ARTIFACT_NAME.capitalize()}".toString(), ":compileKonan", ":build"]
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

    protected void addSetting(String container, String section, String parameter, String value) {
        buildFile.append("$container['$section'].$parameter $value\n")
    }

    protected void addSetting(String container, String section, String parameter, File value) {
        addSetting(container, section, parameter, "'${value.canonicalPath.replace('\\', '\\\\')}'")
    }

    void addCompilationSetting(String artifactName = DEFAULT_ARTIFACT_NAME, String parameter, String value) {
        addSetting("konanArtifacts", artifactName, parameter, value)
    }

    void addCompilationSetting(String artifactName = DEFAULT_ARTIFACT_NAME, String parameter, File value) {
        addSetting("konanArtifacts", artifactName, parameter, value)
    }

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

    static String DEFAULT_INTEROP_NAME = "stdio"

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
                $DEFAULT_INTEROP_NAME { }
            }
            
            konanArtifacts {
                $DEFAULT_ARTIFACT_NAME { useInterop '$DEFAULT_INTEROP_NAME' }
            }
            """.stripIndent()
        )
        interopTasks = [":gen${DEFAULT_INTEROP_NAME.capitalize()}InteropStubs".toString(),
                        ":compile${DEFAULT_INTEROP_NAME.capitalize()}InteropStubs".toString()]
        compilationTasks = [":compileKonan${DEFAULT_ARTIFACT_NAME.capitalize()}".toString(), ":compileKonan", ":build"]
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
            """.stripIndent()
        )
    }

    void addInteropSetting(String interopName = DEFAULT_INTEROP_NAME, String parameter, String value) {
        addSetting("konanInterop", interopName, parameter, value)
    }

    void addInteropSetting(String interopName = DEFAULT_INTEROP_NAME, String parameter, File value) {
        addSetting("konanInterop", interopName, parameter, value)
    }

    static KonanInteropProject create(TemporaryFolder projectDir) {
        return createEmpty(projectDir) {
            it.generateSrcFile("main.kt")
            it.generateDefFile("${DEFAULT_INTEROP_NAME}.def")
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