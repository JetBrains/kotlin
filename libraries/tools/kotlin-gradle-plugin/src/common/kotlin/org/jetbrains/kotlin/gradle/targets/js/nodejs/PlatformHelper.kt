package org.jetbrains.kotlin.gradle.targets.js.nodejs

import java.io.File
import java.util.concurrent.TimeUnit

object PlatformHelper {
    val osName: String by lazy {
        val name = property("os.name").toLowerCase()
        when {
            name.contains("windows") -> "win"
            name.contains("mac") -> "darwin"
            name.contains("linux") -> "linux"
            name.contains("freebsd") -> "linux"
            name.contains("sunos") -> "sunos"
            else -> error("Unsupported OS: $name")
        }
    }


    val osArch: String by lazy {
        val arch = property("os.arch").toLowerCase()
        when {
            /*
             * As Java just returns "arm" on all ARM variants, we need a system call to determine the exact arch. Unfortunately some JVMs say aarch32/64, so we need an additional
             * conditional. Additionally, the node binaries for 'armv8l' are called 'arm64', so we need to distinguish here.
             */
            arch == "arm" || arch.startsWith("aarch") -> property("uname")
                .let {
                    if (it == "armv8l" || it == "aarch64") {
                        "arm64"
                    } else it
                }
                .let {
                    if (it == "x86_64") {
                        "x64"
                    } else it
                }
            arch == "ppc64le" -> "ppc64le"
            arch == "s390x" -> "s390x"
            arch.contains("64") -> "x64"
            else -> "x86"
        }
    }


    val isWindows: Boolean by lazy { osName == "win" }


    private fun property(name: String): String {
        return getSystemProperty(name) ?:
        // Added so that we can test osArch on Windows and on non-arm systems
        if (name == "uname") execute("uname", "-m")
        else error("Unable to find a value for property [$name].")
    }


    private fun getSystemProperty(name: String): String? {
        return System.getProperty(name);
    }
}

fun computeNpmScriptFile(
    nodeDirProvider: File,
    command: String,
    isWindows: Boolean
): String {
    return nodeDirProvider.let { nodeDir ->
        if (isWindows) nodeDir.resolve("node_modules/npm/bin/$command-cli.js").path
        else nodeDir.resolve("lib/node_modules/npm/bin/$command-cli.js").path
    }
}

fun computeNodeBinDir(
    nodeDirProvider: File,
    isWindows: Boolean
) =
    if (isWindows) nodeDirProvider else nodeDirProvider.resolve("bin")

/**
 * Executes the given command and returns its output.
 *
 * This is based on an aggregate of the answers provided here: [https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code]
 */
private fun execute(vararg args: String, timeout: Long = 60): String {
    return ProcessBuilder(args.toList())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
        .apply { waitFor(timeout, TimeUnit.SECONDS) }
        .inputStream.bufferedReader().readText().trim()
}