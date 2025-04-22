import java.io.File
import java.io.IOException
import java.util.HashSet
import org.gradle.internal.os.OperatingSystem
import java.util.Locale

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true
if (!disableInputsCheck && !OperatingSystem.current().isWindows) {
    tasks.withType<Test>().names.forEach {
        val permissionsTask =
            tasks.register<Task>("permissions${it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}") {
                mustRunAfter(tasks.named("processTestResources"))
                val permissionsTemplateFile = rootProject.file("tests-permissions.template.policy")
                inputs.file(permissionsTemplateFile).withPathSensitivity(PathSensitivity.RELATIVE)
                val policyFileProvider: Provider<RegularFile> = layout.buildDirectory.file("permissions-for-$it.policy")
                outputs.file(policyFileProvider).withPropertyName("policyFile")
                val service = project.extensions.getByType<JavaToolchainService>()
                val rootDirPath = rootDir.canonicalPath
                val gradleUserHomeDir = project.gradle.gradleUserHomeDir.absolutePath
                val inputFiles: Provider<FileCollection> = provider { tasks.named<Test>(it).map { it.inputs.files }.get() }
                val javaVersion = provider { tasks.named<Test>(it).map { it.javaLauncher.get().metadata.languageVersion.asInt() }.get() }
                val defineJDKEnvVariables: List<Int> = listOf(8, 11, 17, 21)
                inputs.property("javaVersion", javaVersion)
                inputs.property(
                    "inputFiles",
                    inputFiles.map { it.files.map { it.absolutePath }.toSortedSet() }
                )

                doFirst {
                    if (!permissionsTemplateFile.exists()) {
                        throw GradleException("Security policy template file not found at: ${permissionsTemplateFile.absolutePath}")
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

                    val addedDirs = HashSet<File>()
                    val inputPermissions = inputFiles.get().files.flatMapTo(HashSet<String>()) { file ->
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
                    }

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
                                }.orNull?.executablePath?.asFile?.parentFile?.parentFile?.parentFile?.parentFile?.canonicalPath
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
            }
        tasks.named<Test>(it).configure {
            val policyFileProvider: Provider<File> = permissionsTask.map { it.outputs.files.singleFile }
            inputs.file(policyFileProvider).withPathSensitivity(PathSensitivity.NONE).withPropertyName("policyFile")
            doFirst {
                val policyFile = policyFileProvider.get()
                println("Security policy for test inputs generated to ${policyFile.absolutePath}")
                jvmArgs(
                    "-Djava.security.policy=${policyFile.absolutePath}",
                    "-Djava.security.debug=failure",
                    "-Djava.security.manager=java.lang.SecurityManager",
                )
            }
        }
    }
}