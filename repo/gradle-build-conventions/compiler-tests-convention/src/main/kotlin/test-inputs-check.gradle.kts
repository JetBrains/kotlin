import java.io.File
import java.io.IOException
import java.util.HashSet
import org.gradle.internal.os.OperatingSystem

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true
// Disable checks on windows until we fix KTI-2322
if (!disableInputsCheck && !OperatingSystem.current().isWindows) {
    tasks.withType<Test>().names.forEach { taskName ->
        tasks.named<Test>(taskName) {
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

            doFirst {
                if (!permissionsTemplateFile.exists()) {
                    throw GradleException("Security policy template file not found at: ${permissionsTemplateFile.absolutePath}")
                }

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
                    return file.parents().map { parent ->
                        """permission java.io.FilePermission "${parent.absolutePath}", "read";"""
                    }.toList()
                }

                fun getJDKFromToolchain(service: JavaToolchainService, version: Int): String {
                    return service.launcherFor {
                        languageVersion.set(JavaLanguageVersion.of(version))
                    }.orNull?.executablePath?.asFile?.parentFile?.parentFile?.parentFile?.parentFile?.absolutePath
                        ?: error("Can't find toolchain for $version")
                }

                val debuggerAgentJar: String? = System.getenv("PROCESS_OPTIONS")
                    ?.split(", ")?.asSequence()
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.find { it.startsWith("-javaagent:") && it.contains("debugger-agent.jar") }
                    ?.removePrefix("-javaagent:")
                    ?.substringBefore("=")

                val javaLibraryPaths = System.getProperty("java.library.path", "")
                    .split(File.pathSeparatorChar)
                    .filterNot { it.isBlank() }
                    .map { """permission java.io.FilePermission "$it/-", "read";""" }

                val addedDirs = HashSet<File>()
                val inputPermissions: Set<String> = inputs.files.flatMapTo(HashSet<String>()) { file ->
                    if (file.isDirectory) {
                        addedDirs.add(file)
                        listOf(
                            """permission java.io.FilePermission "${file.absolutePath}/", "read";""",
                            """permission java.io.FilePermission "${file.absolutePath}/-", "read${
                                // We write to the testData folder from tests...
                                if (file.canonicalPath.contains("/testData/")) ",write" else ""
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
                    } else if (file != null) {
                        val parents = parentsReadPermission(file)
                        listOf(
                            """permission java.io.FilePermission "${file.absolutePath}", "read";""",
                        ) + parents
                    } else emptyList()
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
                                "{{temp_dir}}",
                                (parentsReadPermission(File(tempDir)) +
                                        """permission java.io.FilePermission "$tempDir/-", "read,write,delete";""" +
                                        """permission java.io.FilePermission "$tempDir", "read";"""
                                        ).joinToString("\n    ")
                            )
                            .replace(
                                "{{jdk}}",
                                ((defineJDKEnvVariables + javaVersion.get()).map { version ->
                                    """permission java.io.FilePermission "${getJDKFromToolchain(service, version)}/-", "read,execute";"""
                                }).joinToString("\n    ")
                            )
                            .replace("{{gradle_user_home}}", """$gradleUserHomeDir""")
                            .replace(
                                "{{build_dir}}",
                                """permission java.io.FilePermission "${buildDir.get().asFile.absolutePath}/-", "read,write,execute,delete";"""
                            )
                            .replace("{{java_library_paths}}", javaLibraryPaths.joinToString("\n    "))
                            .replace(
                                "{{debugger_agent_jar}}",
                                debuggerAgentJar?.let { """permission java.io.FilePermission "$it", "read";""" } ?: "")
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
                "-Djava.security.manager=java.lang.SecurityManager",
            )
        }
    }
}