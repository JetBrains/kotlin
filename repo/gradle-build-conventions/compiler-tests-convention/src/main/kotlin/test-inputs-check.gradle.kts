import java.io.File
import java.io.IOException
import java.util.HashSet
import org.gradle.internal.os.OperatingSystem

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true
if (!disableInputsCheck && !OperatingSystem.current().isWindows) {
    tasks.withType<Test>().names.forEach { taskName ->
        tasks.named<Test>(taskName) {
            mustRunAfter(tasks.named("processTestResources"))
            val permissionsTemplateFile = rootProject.file("tests-permissions.template.policy")
            inputs.file(permissionsTemplateFile).withPathSensitivity(PathSensitivity.RELATIVE)
            val policyFileProvider: Provider<RegularFile> = layout.buildDirectory.file("permissions-for-$taskName.policy")
            outputs.file(policyFileProvider).withPropertyName("policyFile")
            val service = project.extensions.getByType<JavaToolchainService>()
            val rootDirPath = rootDir.canonicalPath
            val buildDir = layout.buildDirectory
            val gradleUserHomeDir = project.gradle.gradleUserHomeDir.absolutePath
            val javaVersion = provider { tasks.named<Test>(taskName).map { it.javaLauncher.get().metadata.languageVersion.asInt() }.get() }
            val defineJDKEnvVariables: List<Int> = listOf(8, 11, 17, 21)
            inputs.property("javaVersion", javaVersion)

            doFirst {
                if (!permissionsTemplateFile.exists()) {
                    throw GradleException("Security policy template file not found at: ${permissionsTemplateFile.absolutePath}")
                }

                fun getDebuggerAgentJar(): String? {
                    return System.getenv("PROCESS_OPTIONS")
                        ?.split(", ")?.asSequence()
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.find { it.startsWith("-javaagent:") && it.contains("debugger-agent.jar") }
                        ?.removePrefix("-javaagent:")
                        ?.substringBefore("=")
                }

                fun parentsReadPermission(file: File): List<String> {
                    val parents = mutableListOf<String>()
                    var p: File? = file.parentFile
                    while (p != null && p.canonicalPath != rootDirPath) {
                        parents.add("""permission java.io.FilePermission "${p.absolutePath}", "read";""")
                        p = p.parentFile
                    }
                    return parents
                }

                val javaLibraryPaths = System.getProperty("java.library.path", "")
                    .split(File.pathSeparatorChar)
                    .filterNot { it.isBlank() }
                    .map { """permission java.io.FilePermission "$it${File.separatorChar}-", "read";""" }
                val addedDirs = HashSet<File>()
                val inputPermissions: Set<String> = inputs.files.flatMapTo(HashSet<String>()) { file ->
                    if (file.isDirectory()) {
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
                                addedDirs.add(
                                    file.parentFile
                                )
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
                            """permission java.io.FilePermission "${file.absolutePath}", "read${
                                if (file.extension == "txt") {
                                    ",delete"
                                } else {
                                    ""
                                }
                            }";""",
                        ) + parents
                    } else emptyList()
                } +
                        """permission java.io.FilePermission "${buildDir.get().asFile.absolutePath}/-", "read,write,execute,delete";""" +
                        javaLibraryPaths +
                        (getDebuggerAgentJar()?.let { """permission java.io.FilePermission "$it", "read";""" } ?: "")

                fun calcCanonicalTempPath(): String {
                    val file = File(System.getProperty("java.io.tmpdir"))
                    try {
                        val canonical = file.getCanonicalPath()
                        if (!OperatingSystem.current().isWindows || !canonical.contains(" ")) {
                            return canonical
                        }
                    } catch (_: IOException) {
                    }
                    return file.absolutePath
                }

                val temp_dir = calcCanonicalTempPath()

                val policyFile = policyFileProvider.get().asFile
                policyFile.parentFile.mkdirs()
                try {
                    policyFile.writeText(
                        permissionsTemplateFile.readText().replace(
                            "{{temp_dir}}",
                            (parentsReadPermission(File(temp_dir)) + """permission java.io.FilePermission "$temp_dir/-", "read,write,delete";""" + """permission java.io.FilePermission "$temp_dir", "read";""").joinToString(
                                "\n    "
                            )
                        ).replace("{{jdk}}", ((defineJDKEnvVariables + javaVersion.get()).map { version ->
                            val jdkHome = service.launcherFor {
                                languageVersion.set(JavaLanguageVersion.of(version))
                            }.orNull?.executablePath?.asFile?.parentFile?.parentFile?.parentFile?.parentFile?.absolutePath
                                ?: error("Can't find toolchain for $version")
                            """permission java.io.FilePermission "$jdkHome/-", "read,execute";"""
                        }).joinToString("\n    ")).replace(
                            "{{gradle_user_home}}", """$gradleUserHomeDir"""
                        ).replace("{{inputs}}", inputPermissions.sorted().joinToString("\n    ")))
                } catch (e: IOException) {
                    logger.error("Failed to generate security policy file", e)
                    throw e
                }
            }
            println("Security policy for test inputs generated to ${policyFileProvider.get().asFile.absolutePath}")
            jvmArgs(
                "-Djava.security.policy=${policyFileProvider.get().asFile.absolutePath}",
                "-Djava.security.debug=failure",
                "-Djava.security.manager=java.lang.SecurityManager",
            )
        }
    }
}