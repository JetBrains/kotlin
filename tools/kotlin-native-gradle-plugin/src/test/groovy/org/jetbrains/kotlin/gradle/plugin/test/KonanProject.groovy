package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.GradleRunner

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class KonanProject {

    static String DEFAULT_ARTIFACT_NAME = 'main'

    File projectDir
    Path projectPath
    File konanBuildDir

    String konanHome

    File         buildFile
    File         propertiesFile
    File         settingsFile

    Set<File>    srcFiles = []

    List<String> compilationTasks = []
    String       downloadTask = ":downloadKonanCompiler"

    List<String> getBuildingTasks() { return compilationTasks }
    List<String> getKonanTasks()    { return getBuildingTasks() + downloadTask }

    protected KonanProject(File projectDir) {
        this.projectDir = projectDir
        projectPath = projectDir.toPath()
        konanBuildDir = projectPath.resolve('build/konan').toFile()
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

    GradleRunner createRunner(boolean withDebug = true) {
        return GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().withDebug(withDebug)
    }

    /** Creates a subdirectory specified by the given path. */
    File createSubDir(String ... path) {
        return createSubDir(Paths.get(*path))
    }

    /** Creates a subdirectory specified by the given path. */
    File createSubDir(Path path) {
        return Files.createDirectories(projectPath.resolve(path)).toFile()
    }

    /** Creates a file with the given content in project subdirectory specified by parentDirectory. */
    File createFile(Path parentDirectory = projectPath, String fileName, String content) {
        def parent = projectPath.resolve(parentDirectory)
        Files.createDirectories(parent)
        def result = parent.resolve(fileName).toFile()
        result.createNewFile()
        result.write(content)
        return result
    }

    /** Creates a file with the given content in project subdirectory specified by parentPath. */
    File createFile(List<String> parentPath, String fileName, String content) {
        return createFile(Paths.get(*parentPath), fileName, content)
    }

    /** Creates a folder for project source files (src/main/kotlin). */
    void generateFolders() {
        createSubDir("src", "main", "kotlin")
    }

    /** Generates a build.gradle file in the root project directory with the given content. */
    File generateBuildFile(String content) {
        buildFile = createFile(projectPath, "build.gradle", content)
        return buildFile
    }

    /** Generates a settings.gradle file in the root project directory with the given content. */
    File generateSettingsFile(String content) {
        settingsFile = createFile(projectPath, "settings.gradle", content)
        return settingsFile
    }

    /**
     * Generates a build.gradle file in root project directory with the default content (see below)
     * and fills the compilationTasks array.
     *
     *  plugins { id 'konan' }
     *
     *  konanArtifacts {
     *      $DEFAULT_ARTIFACT_NAME { }
     *  }
     */
    File generateBuildFile() {
        def result = generateBuildFile("""
            plugins { id 'konan' }
            
            konanArtifacts {
                $DEFAULT_ARTIFACT_NAME { }
            }
            """.stripIndent()
        )
        compilationTasks = [defaultCompilationTask(), ":compileKonan", ":build"]
        return result
    }

    /** Generates a source file with the given name and content in the given directory and adds it into srcFiles */
    File generateSrcFile(Path parentDirectory, String fileName, String content) {
        def result = createFile(parentDirectory, fileName, content)
        srcFiles.add(result)
        return result
    }

    /** Generates a source file with the given name and content in the given directory and adds it into srcFiles */
    File generateSrcFile(List<String> parentPath, String fileName, String content) {
        return generateSrcFile(Paths.get(*parentPath), fileName, content)
    }

    /** Generates a source file with the given name and content in 'src/main/kotlin' and adds it into srcFiles */
    File generateSrcFile(String fileName, String content) {
        return generateSrcFile(["src", "main", "kotlin"], fileName, content)
    }

    /**
     * Generates a source file with the given name and default content (see below) in src/main/kotlin
     * and adds it into srcFiles.
     *
     *  fun main(args: Array<String>) {
     *      println(42)
     *  }
     */
    File generateSrcFile(String fileName) {
        return generateSrcFile(fileName, """           
            fun main(args: Array<String>) {
                println(42)
            }
            """.stripIndent()
        )
    }

    /** Generates gradle.properties file with the konan.home property set. */
    File generatePropertiesFile(String konanHome) {
        propertiesFile = createFile(projectPath, "gradle.properties", "konan.home=$konanHome\n")
        return propertiesFile
    }

    /**
     * Sets the given setting of the given project extension.
     * In other words adds the following string in the build file:
     *
     *  $container['$section'].$parameter $value
     */
    protected void addSetting(String container, String section, String parameter, String value) {
        buildFile.append("$container['$section'].$parameter $value\n")
    }

    /**
     * Sets the given setting of the given project extension using the path of the file as a value.
     * In other words adds the following string in the build file:
     *
     *  $container['$section'].$parameter ${value.canonicalPath.replace(\, \\)}
     */
    protected void addSetting(String container, String section, String parameter, File value) {
        addSetting(container, section, parameter, "'${value.canonicalPath.replace('\\', '\\\\')}'")
    }

    /** Sets the given setting of the given konanArtifact */
    void addCompilationSetting(String artifactName = DEFAULT_ARTIFACT_NAME, String parameter, String value) {
        addSetting("konanArtifacts", artifactName, parameter, value)
    }

    /** Sets the given setting of the given konanArtifact using the path of the file as a value. */
    void addCompilationSetting(String artifactName = DEFAULT_ARTIFACT_NAME, String parameter, File value) {
        addSetting("konanArtifacts", artifactName, parameter, value)
    }

    /** Returns the path of compileKonan... task for the default artifact. */
    String defaultCompilationTask() {
        return compilationTask(DEFAULT_ARTIFACT_NAME)
    }

    /** Returns the path of compileKonan... task for the artifact specified. */
    String compilationTask(String artifactName) {
        return ":compileKonan${artifactName.capitalize()}"
    }

    /** Creates a project with default build and source files. */
    static KonanProject create(File projectDir) {
        return createEmpty(projectDir) {
            it.generateSrcFile("main.kt")
        }
    }

    /** Creates a project with default build and source files. */
    static KonanProject create(File projectDir, Closure config) {
        def result = create(projectDir)
        config(result)
        return result
    }

    /** Creates a project with the default build file and without any source files. */
    static KonanProject createEmpty(File projectDir) {
        def result = new KonanProject(projectDir)
        result.with {
            generateFolders()
            generateBuildFile()
            generatePropertiesFile(konanHome)
            generateSettingsFile("")
        }
        return result
    }

    /** Creates a project with the default build file and without any source files. */
    static KonanProject createEmpty(File projectDir, Closure config) {
        def result = createEmpty(projectDir)
        config(result)
        return result
    }

}

class KonanInteropProject extends KonanProject {

    static String DEFAULT_INTEROP_NAME = "stdio"

    Set<File> defFiles = []

    List<String> interopTasks = []

    protected KonanInteropProject(File projectDir) { super(projectDir) }

    List<String> getBuildingTasks() { return interopTasks + compilationTasks }

    /** Creates a folder for project source files (src/main/kotlin) and a folder for def-files (src/main/c_interop). */
    void generateFolders() {
        super.generateFolders()
        createSubDir("src", "main", "c_interop")
    }

    /**
     * Generates a build.gradle file in root project directory with the default content (see below)
     * and fills compilationTasks and interopTasks arrays.
     *
     *  plugins { id 'konan' }
     *
     *  konanInterop {
     *      $DEFAULT_INTEROP_NAME { }
     *  }
     *
     *  konanArtifacts {
     *      $DEFAULT_ARTIFACT_NAME { useInterop '$DEFAULT_INTEROP_NAME' }
        }
     */
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
        interopTasks = [defaultStubGenerationTask()]
        compilationTasks = [defaultCompilationTask(), ":compileKonan", ":build"]
        return result
    }

    /** Creates a def-file with the given name and content in src/main/c_interop directory and adds it to defFiles. */
    File generateDefFile(String fileName, String content) {
        def result = createFile(["src", "main", "c_interop"], fileName, content)
        defFiles.add(result)
        return result
    }

    /**
     * Creates a def-file with the given name and the default content (see below) in src/main/c_interop directory
     * and adds it to defFiles.
     *
     *  headers = stdio.h stdlib.h string.h
     */
    File generateDefFile(String fileName = "${DEFAULT_INTEROP_NAME}.def") {
        return generateDefFile(fileName, """
            headers = stdio.h stdlib.h string.h
            """.stripIndent()
        )
    }

    /** Sets the given setting of the given konanInterop */
    void addInteropSetting(String interopName = DEFAULT_INTEROP_NAME, String parameter, String value) {
        addSetting("konanInterop", interopName, parameter, value)
    }

    /** Sets the given setting of the given konanInterop using the path of the file as a value. */
    void addInteropSetting(String interopName = DEFAULT_INTEROP_NAME, String parameter, File value) {
        addSetting("konanInterop", interopName, parameter, value)
    }

    String defaultStubGenerationTask() {
        return stubGenerationTask(DEFAULT_INTEROP_NAME)
    }

    String stubGenerationTask(String interopName) {
        return ":process${interopName.capitalize()}Interop"
    }

    /** Creates a project with default build, source and def files. */
    static KonanInteropProject create(File projectDir) {
        return createEmpty(projectDir) {
            it.generateSrcFile("main.kt")
            it.generateDefFile("${DEFAULT_INTEROP_NAME}.def")
        }
    }

    /** Creates a project with default build, source and def files. */
    static KonanInteropProject create(File projectDir, Closure config) {
        def result = create(projectDir)
        config(result)
        return result
    }

    /** Creates a project with the default build file, without any source and with an empty def file. */
    static KonanInteropProject createEmpty(File projectDir) {
        def result = new KonanInteropProject(projectDir)
        result.with {
            generateFolders()
            generateBuildFile()
            generatePropertiesFile(konanHome)
            generateDefFile("${DEFAULT_INTEROP_NAME}.def", "")
        }
        return result
    }

    /** Creates a project with the default build file, without any source and with an empty def file. */
    static KonanInteropProject createEmpty(File projectDir, Closure config) {
        def result = createEmpty(projectDir)
        config(result)
        return result
    }
}