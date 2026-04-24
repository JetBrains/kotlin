import org.gradle.internal.os.OperatingSystem
import org.gradle.process.CommandLineArgumentProvider
import java.io.IOException

plugins {
    java
}

abstract class TestInputsCheckArgumentProvider : CommandLineArgumentProvider {
    @get:Input
    abstract val enabled: Property<Boolean>

    @get:Internal
    abstract val policyFile: RegularFileProperty

    override fun asArguments(): Iterable<String> =
        if (enabled.get()) {
            listOf(
                "-Djava.security.policy=${policyFile.get().asFile.absolutePath}",
                "-Djava.security.manager=org.jetbrains.kotlin.security.KotlinSecurityManager",
            )
        } else {
            emptyList()
        }
}

dependencies {
    testImplementation(project(":compiler:test-security-manager"))
}

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true
val inputsCheckIsSupported = !disableInputsCheck && !OperatingSystem.current().isWindows
tasks.withType<Test>().configureEach {
    val taskName = this.name
    ignoreFailures = false
    val testInputsCheck = extensions.create<TestInputsCheckExtension>("testInputsCheck")
    val toolchainPath = testInputsCheck.isNative
        .filter { it }
        .flatMap {
            project.providers.of(XcodeToolchainValueSource::class.java) {}
        }

    outputs.doNotCacheIf(
        "test-inputs-check is disabled or unsupported for the task",
        TestInputsCheckCacheSpec(inputsCheckIsSupported, testInputsCheck.enabled),
    )

    // Disable checks on windows until we fix KTI-2322
    if (inputsCheckIsSupported) {
        val permissionsTemplateFile = rootProject.file("tests-permissions.template.policy")
        inputs.file(permissionsTemplateFile).withPathSensitivity(PathSensitivity.RELATIVE)
        val policyFileProvider: Provider<RegularFile> = layout.buildDirectory.file("permissions-for-$taskName.policy")
        outputs.file(policyFileProvider).withPropertyName("policyFile")
        val service = project.extensions.getByType<JavaToolchainService>()
        val buildDir = layout.buildDirectory
        val gradleUserHomeDir = project.gradle.gradleUserHomeDir.absolutePath
        val javaVersion = provider { tasks.named<Test>(taskName).map { it.javaLauncher.get().metadata.languageVersion.asInt() }.get() }
        val defineJDKEnvVariables: List<Int> = listOf(8, 11, 17, 21)
        inputs.property("javaVersion", javaVersion)
        val nativeHome = project.providers.gradleProperty("kotlin.internal.native.test.nativeHome").orElse(
            project.providers.gradleProperty("kn.nativeHome")
        )
        val nativeHomeDefault: Provider<Directory> =
            testInputsCheck.isNative.filter { it }.map { project.project(":kotlin-native").isolated.projectDirectory.dir("dist") }
        val konanDataDir: String =
            project.extra.has("konan.data.dir").let { if (it) project.extra["konan.data.dir"] else null } as String?
                ?: System.getenv("KONAN_DATA_DIR")
                ?: (System.getProperty("user.home") + File.separator + ".konan")

        @Suppress("UNCHECKED_CAST")
        val d8Executable = if (project.extra.has("javascript.engine.path.V8")) {
            project.extra["javascript.engine.path.V8"] as Provider<String>
        } else null

        @Suppress("UNCHECKED_CAST")
        val nodeJsExecutable = if (project.extra.has("javascript.engine.path.NodeJs")) {
            project.extra["javascript.engine.path.NodeJs"] as Provider<String>
        } else null

        @Suppress("UNCHECKED_CAST")
        val binaryenExecutable = if (project.extra.has("binaryen.path")) {
            project.extra["binaryen.path"] as Provider<String>
        } else null

        doFirst {
            if (!testInputsCheck.enabled.get()) return@doFirst

            if (!permissionsTemplateFile.exists()) {
                throw GradleException("Security policy template file not found at: ${permissionsTemplateFile.absolutePath}")
            }
            val addedDirs = HashSet<File>()

            fun File.parents(): Sequence<File> {
                return sequence {
                    var f: File? = this@parents.parentFile
                    while (f != null) {
                        yield(f)
                        f = f.parentFile
                    }
                }
            }

            fun parentsReadPermission(file: File): List<String> {
                return file.parents().mapNotNull { parent ->
                    """permission java.io.FilePermission "${parent.absolutePath}", "read";""".takeIf { addedDirs.add(parent) }
                }.toList()
            }

            val coarseInputDirectories = testInputsCheck.coarseInputDirectories.files.map { it.canonicalFile }
            val coarseInputDirectoryPaths = coarseInputDirectories.map { it.toPath().toAbsolutePath().normalize() }

            fun File.isInOrUnderCoarseInputDirectory(): Boolean {
                val path = absoluteFile.toPath().normalize()
                return coarseInputDirectoryPaths.any { directoryPath -> path == directoryPath || path.startsWith(directoryPath) }
            }

            fun getJDKFromToolchain(service: JavaToolchainService, version: Int): String {
                return service.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(version))
                }.orNull?.executablePath?.asFile?.parentFile?.parentFile?.parentFile?.parentFile?.absolutePath
                    ?: error("Can't find toolchain for $version")
            }

            val debuggerAgentPath: String? = System.getenv("PROCESS_OPTIONS")
                ?.split(", ")?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.find { it.startsWith("-javaagent:") && it.contains("debugger-agent.jar") }
                ?.removePrefix("-javaagent:")
                ?.substringBefore("=")
                ?.removeSuffix("/debugger-agent.jar")

            val javaLibraryPaths = System.getProperty("java.library.path", "")
                .split(File.pathSeparatorChar)
                .filterNot { it.isBlank() }
                .flatMap {
                    listOf(
                        // libcallbacks was renamed after 2.3.0
                        // different versions of different stages will try to access different names of the libs during testing
                        // keep both old and new names here
                        """permission java.io.FilePermission "$it/libcallbacks.dylib", "read";""",
                        """permission java.io.FilePermission "$it/libcallbacks.so", "read";""",
                        """permission java.io.FilePermission "$it/libcallbacks.dll", "read";""",
                        """permission java.io.FilePermission "$it/libkotlinxcinteropjvmcallbacks.dylib", "read";""",
                        """permission java.io.FilePermission "$it/libkotlinxcinteropjvmcallbacks.so", "read";""",
                        """permission java.io.FilePermission "$it/libkotlinxcinteropjvmcallbacks.dll", "read";""",

                        """permission java.io.FilePermission "$it/libclangstubs.dylib", "read";""",
                        """permission java.io.FilePermission "$it/libclangstubs.so", "read";""",
                        """permission java.io.FilePermission "$it/libclangstubs.dll", "read";""",
                        """permission java.io.FilePermission "$it/libllvmstubs.dylib", "read";""",
                        """permission java.io.FilePermission "$it/libllvmstubs.so", "read";""",
                        """permission java.io.FilePermission "$it/libllvmstubs.dll", "read";""",
                    )
                }

            val coarseInputDirectoryPermissions: Set<String> = coarseInputDirectories.flatMapTo(HashSet()) { directory ->
                addedDirs.add(directory)
                parentsReadPermission(directory) + listOf(
                    // Allow the task to check the declared coarse input directory root itself.
                    """permission java.io.FilePermission "${directory.absolutePath}", "read";""",
                    // Allow the task to list direct children of the declared coarse input directory.
                    """permission java.io.FilePermission "${directory.absolutePath}/", "read";""",
                    // Allow the task to read files under the declared coarse input without enumerating the whole tree.
                    """permission java.io.FilePermission "${directory.absolutePath}/-", "read";""",
                )
            }

            val inputPermissions: Set<String> = inputs.files
                .filterNot { file -> file.isInOrUnderCoarseInputDirectory() }
                .flatMapTo(HashSet()) { file ->
                    if (file.isDirectory) {
                        addedDirs.add(file)
                        buildList {
                            add("""permission java.io.FilePermission "${file.absolutePath}/", "read";""")
                            if (file.canonicalPath.contains("/testData")) {
                                // We write to the testData folder from tests...
                                add("""permission java.io.FilePermission "${file.absolutePath}/-", "read,write,delete";""")
                            } else {
                                add("""permission java.io.FilePermission "${file.absolutePath}/-", "read";""")
                            }
                            if (file.canonicalPath.endsWith("dist")) {
                                add("""permission java.io.FilePermission "${file.resolve("kotlinc").resolve("bin")}/-", "read,execute";""")
                            }
                        }
                    } else if (file.extension == "class") {
                        listOfNotNull(
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}/-", "read";""".takeIf {
                                addedDirs.add(file.parentFile)
                            }
                        )
                    } else if (file.extension == "jar") {
                        listOf(
                            // JvmCompilationUtils.compileJavaFiles uses embedded javaCompiler if no jdkHome is set, and it opens dependencies
                            """permission java.io.FilePermission "${file.absolutePath}", "read,write";""",
                            """permission java.io.FilePermission "${file.absolutePath}/-", "read";""",
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}", "read";""",
                        )
                    } else if (file.extension == "klib") {
                        // KlibLoader.kt creates a ZipFileSystem, and that always require write permission
                        // (even if you don't modify the file, potentially, you could)
                        listOf(
                            """permission java.io.FilePermission "${file.absolutePath}", "read,write";""",
                        )
                    } else if (file.parentFile.name == "ideaHomeForTests") {
                        listOf(
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}/-", "read,write";""",
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}", "read";""",
                        )
                    } else {
                        val parents = parentsReadPermission(file)
                        listOf(
                            """permission java.io.FilePermission "${file.absolutePath}", "read";""",
                        ) + parents
                    }
                }
            val allInputPermissions = coarseInputDirectoryPermissions + inputPermissions

            val permissionsForGradleRoDepCache = System.getenv("GRADLE_RO_DEP_CACHE")?.let {
                val cacheDir = File(it).absoluteFile
                listOf(
                    """grant codeBase "file:${cacheDir.absolutePath}/-" {""",
                    """    permission java.security.AllPermission;""",
                    """};""",
                    """grant {""",
                    """    permission java.io.FilePermission "${cacheDir.absolutePath}", "read";""",
                    """    permission java.io.FilePermission "${cacheDir.absolutePath}/-", "read";""",
                    """};""",
                ).joinToString("\n")
            }

            fun calcCanonicalTempPath(): String {
                val file = File(System.getProperty("java.io.tmpdir"))
                try {
                    val canonical = file.canonicalPath
                    if (!OperatingSystem.current().isWindows || !canonical.contains(" ")) {
                        return canonical
                    }
                } catch (_: IOException) {
                }
                return file.absolutePath
            }

            val tempDir = calcCanonicalTempPath()

            val policyFile = policyFileProvider.get().asFile
            policyFile.parentFile.mkdirs()
            try {
                policyFile.writeText(
                    permissionsTemplateFile.readText()
                        .replace(
                            "{{native}}",
                            if (testInputsCheck.isNative.get()) {
                                val konanPermissions: MutableList<String> = mutableListOf(
                                    """permission java.util.PropertyPermission "kotlin.native.home", "write";""",
                                    """permission java.util.PropertyPermission "kotlinc.test.allow.testonly.language.features", "write";""",

                                    //This is scary because it's too broad
                                    """permission java.util.PropertyPermission "*", "write";""", // org/jetbrains/kotlin/konan/test/blackbox/support/util/SafeEnvironment.kt:48

                                    """permission java.io.FilePermission "$konanDataDir/-", "read,write,delete,execute";""",
                                    """permission java.io.FilePermission "$konanDataDir", "read";""",
                                    """permission java.io.FilePermission "/bin/sh", "execute";""",
                                    """permission java.io.FilePermission "/bin/tar", "execute";""",
                                    """permission java.io.FilePermission "/usr/bin/tar", "execute";""",
                                    """permission java.io.FilePermission "${nativeHome.getOrElse(nativeHomeDefault.get().asFile.absolutePath)}/-" , "read,write,delete";""",
                                    """permission java.io.FilePermission "${nativeHome.getOrElse(nativeHomeDefault.get().asFile.absolutePath)}" , "read";""",
                                    """permission java.io.FilePermission "<<ALL FILES>>", "execute";""", // DependencyExtractor.kt to untar calls `tar` directly, and the system needs to find it
                                    """permission java.net.SocketPermission "download.jetbrains.com:443", "connect,resolve";""", // DependencyDownloader.kt
                                    """permission java.net.SocketPermission "download-cdn.jetbrains.com:443", "connect,resolve";""", // DependencyDownloader.kt
                                    """permission java.net.SocketPermission "repo.labs.intellij.net:443", "connect,resolve";""", // DependencyDownloader.kt
                                    // add link permission to load jvmcallbacks library, via possible invocation of `JvmUtilsKt.createTempDirWithLibrary()` which invokes `Files.createLink()`
                                    // This happens in case of `catch (e: UnsatisfiedLinkError)` in `JvmUtilsKt.tryLoadKonanLibrary()`
                                    // with message `Native Library <...>/kotlin-native/dist/konan/nativelib/libkotlinxcinteropjvmcallbacks.dylib already loaded in another classloader`
                                    """permission java.nio.file.LinkPermission "hard";""",
                                )
                                if (nativeHome.isPresent) {
                                    konanPermissions.add("""permission java.io.FilePermission "${nativeHome.get()}/-" , "read,write,delete";""")
                                }
                                if (OperatingSystem.current().isMacOsX) {
                                    // Should we consider those files inputs? I need to think about the execute permission
                                    // in any case I need to check where those paths come from to avoid hardcoding
                                    konanPermissions.addAll(
                                        listOf(
                                            """permission java.io.FilePermission "/bin/bash", "execute";""",
                                            """permission java.io.FilePermission "/usr/bin/xcrun", "execute";""",
                                            """permission java.io.FilePermission "/usr/bin/codesign", "execute";""", // CompilationToolCall.kt:274
                                            """permission java.io.FilePermission "${toolchainPath.get()}/-", "read,execute";""",
                                        )
                                    )
                                }
                                konanPermissions.joinToString("\n    ")
                            } else ""
                        )
                        .replace(
                            "{{temp_dir}}",
                            listOf(tempDir, System.getProperty("java.io.tmpdir")).flatMap {
                                parentsReadPermission(File(it)) +
                                        """permission java.io.FilePermission "$it/-", "read,write,delete";""" +
                                        """permission java.io.FilePermission "$it", "read";"""
                            }.joinToString("\n    ")
                        )
                        .replace(
                            "{{jdk}}",
                            ((defineJDKEnvVariables + javaVersion.get()).distinct().map { version ->
                                """permission java.io.FilePermission "${getJDKFromToolchain(service, version)}/-", "read,execute";"""
                            }).joinToString("\n    ")
                        )
                        .replace(
                            "{{flight_recorder}}",
                            buildString {
                                if (testInputsCheck.allowFlightRecorder.get()) {
                                    append("""permission jdk.jfr.FlightRecorderPermission "registerEvent";""")
                                }
                            }
                        )
                        .replace("{{gradle_user_home}}", """$gradleUserHomeDir""")
                        .replace("{{all_permissions_for_gradle_ro_dep_cache}}", permissionsForGradleRoDepCache ?: "")
                        .replace(
                            "{{build_dir}}",
                            buildString {
                                val buildDirPath = buildDir.get().asFile.absolutePath
                                append("""permission java.io.FilePermission "$buildDirPath/-", "read,write,execute,delete";""")
                                // Gradle's FileSystemProbing uppercases a filename and calls File.exists() to detect
                                // filesystem case-sensitivity. SecurityManager.checkRead fires on the uppercased path
                                // regardless of OS or whether the file exists, so grant read for that variant too.
                                val buildDirUpper = buildDirPath.uppercase()
                                if (buildDirUpper != buildDirPath) {
                                    append("\n    ")
                                    append("""permission java.io.FilePermission "$buildDirUpper/-", "read";""")
                                }
                            }
                        )
                        .replace("{{java_library_paths}}", javaLibraryPaths.joinToString("\n    "))
                        .replace(
                            "{{debugger_agent_jar}}",
                            debuggerAgentPath?.let { """permission java.io.FilePermission "$it/-", "read";""" } ?: "")
                        .replace("{{inputs}}", allInputPermissions.sorted().joinToString("\n    "))
                        .replace(
                            "{{wasm}}",
                            buildString {
                                d8Executable?.let {
                                    append("""permission java.io.FilePermission "${it.get()}", "execute";""")
                                }
                                nodeJsExecutable?.let {
                                    append("""permission java.io.FilePermission "${it.get()}", "execute";""")
                                }
                                binaryenExecutable?.let {
                                    append("""permission java.io.FilePermission "${it.get()}", "execute";""")
                                }
                            }
                        )
                        .replace(
                            "{{js}}",
                            buildString {
                                d8Executable?.let {
                                    append("""permission java.io.FilePermission "${it.get()}", "execute";""")
                                }
                            }
                        )
                        .replace(
                            "{{extra_permissions}}",
                            testInputsCheck.extraPermissions.get().joinToString("\n")
                        )

                )
            } catch (e: IOException) {
                logger.error("Failed to generate security policy file", e)
                throw e
            }
        }
        logger.info("Security policy for test inputs generated to ${policyFileProvider.get().asFile.absolutePath}")
        jvmArgumentProviders.add(
            objects.newInstance(TestInputsCheckArgumentProvider::class.java).apply {
                enabled.set(testInputsCheck.enabled)
                policyFile.set(policyFileProvider)
            }
        )
    }
}
