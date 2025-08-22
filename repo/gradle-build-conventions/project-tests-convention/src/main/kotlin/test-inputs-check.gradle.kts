import java.io.File
import java.io.IOException
import java.util.HashSet
import org.gradle.internal.os.OperatingSystem

dependencies {
    "testImplementation"(project(":compiler:test-security-manager"))
}

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true
tasks.withType<Test>().names.forEach { taskName ->
    tasks.named<Test>(taskName) {
        ignoreFailures = false
        val testInputsCheck = extensions.create<TestInputsCheckExtension>("testInputsCheck")
        val toolchainPath = testInputsCheck.isNative
            .filter { it }
            .flatMap {
                project.providers.of(XcodeToolchainValueSource::class.java) {}
            }

        // Disable checks on windows until we fix KTI-2322
        if (!disableInputsCheck && !OperatingSystem.current().isWindows) {
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

            doFirst {
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
                            """permission java.io.FilePermission "$it/libcallbacks.dylib", "read";""",
                            """permission java.io.FilePermission "$it/libcallbacks.so", "read";""",
                            """permission java.io.FilePermission "$it/libcallbacks.dll", "read";""",
                            """permission java.io.FilePermission "$it/libclangstubs.dylib", "read";""",
                            """permission java.io.FilePermission "$it/libclangstubs.so", "read";""",
                            """permission java.io.FilePermission "$it/libclangstubs.dll", "read";""",
                            """permission java.io.FilePermission "$it/libllvmstubs.dylib", "read";""",
                            """permission java.io.FilePermission "$it/libllvmstubs.so", "read";""",
                            """permission java.io.FilePermission "$it/libllvmstubs.dll", "read";""",
                        )
                    }

                val inputPermissions: Set<String> = inputs.files.flatMapTo(HashSet<String>()) { file ->
                    if (file.isDirectory) {
                        addedDirs.add(file)
                        listOf(
                            """permission java.io.FilePermission "${file.absolutePath}/", "read";""",
                            """permission java.io.FilePermission "${file.absolutePath}/-", "read${
                                // We write to the testData folder from tests...
                                if (file.canonicalPath.contains("/testData")) ",write"
                                else ""
                            }";""",
                        )
                    } else if (file.extension == "class") {
                        listOfNotNull(
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}/-", "read";""".takeIf {
                                addedDirs.add(file.parentFile)
                            }
                        )
                    } else if (file.extension == "jar") {
                        listOf(
                            """permission java.io.FilePermission "${file.absolutePath}", "read";""",
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}", "read";""",
                        )
                    } else if (file.parentFile.name == "ideaHomeForTests") {
                        listOf(
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}/-", "read,write";""",
                            """permission java.io.FilePermission "${file.parentFile.absolutePath}", "read";""",
                        )
                    } else if (file != null) {
                        val parents = parentsReadPermission(file)
                        listOf(
                            """permission java.io.FilePermission "${file.absolutePath}", "read";""",
                        ) + parents
                    } else emptyList()
                }

                val allPermissionsForGradleRoDepCache = System.getenv("GRADLE_RO_DEP_CACHE")?.let {
                    listOf(
                        """grant codeBase "file:${File(it).absolutePath}/-" {""",
                        """    permission java.security.AllPermission;""",
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
                                    )
                                    if (nativeHome.isPresent) {
                                        konanPermissions.add("""permission java.io.FilePermission "${nativeHome.get()}/-" , "read,write,delete";""")
                                    }
                                    if (testInputsCheck.useXcode.get()) {
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
                            .replace("{{gradle_user_home}}", """$gradleUserHomeDir""")
                            .replace("{{all_permissions_for_gradle_ro_dep_cache}}", allPermissionsForGradleRoDepCache ?: "")
                            .replace(
                                "{{build_dir}}",
                                """permission java.io.FilePermission "${buildDir.get().asFile.absolutePath}/-", "read,write,execute,delete";"""
                            )
                            .replace("{{java_library_paths}}", javaLibraryPaths.joinToString("\n    "))
                            .replace(
                                "{{debugger_agent_jar}}",
                                debuggerAgentPath?.let { """permission java.io.FilePermission "$it/-", "read";""" } ?: "")
                            .replace("{{inputs}}", inputPermissions.sorted().joinToString("\n    "))
                    )
                } catch (e: IOException) {
                    logger.error("Failed to generate security policy file", e)
                    throw e
                }
            }
            logger.info("Security policy for test inputs generated to ${policyFileProvider.get().asFile.absolutePath}")
            jvmArgs(
                "-Djava.security.policy=${policyFileProvider.get().asFile.absolutePath}",
                "-Djava.security.debug=failure",
                "-Djava.security.manager=org.jetbrains.kotlin.security.KotlinSecurityManager",
            )
        }
    }
}
