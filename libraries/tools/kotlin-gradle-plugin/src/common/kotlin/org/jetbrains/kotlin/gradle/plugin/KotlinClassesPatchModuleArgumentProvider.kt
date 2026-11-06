/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.NormalizeLineEndings
import java.io.File
import java.nio.charset.Charset

/**
 * Passes `--patch-module <module>=<Kotlin classes>` to javac when the Java sources include a `module-info.java`.
 *
 * Without it, javac compiles the named module without the Kotlin classes, which stay on the classpath and are not
 * readable from the module (KT-55389).
 *
 * The arguments are computed when javac arguments are assembled, after every `doFirst` action of the task, so that a
 * `--patch-module` added by one of them is seen.
 */
internal class KotlinClassesPatchModuleArgumentProvider(
    javaSources: FileTree,
    @get:CompileClasspath val kotlinClasses: FileCollection,
    // The destination directory is an output of the task
    @get:Internal val javaClassesDir: Provider<Directory>,
    // The parts of the options read here, the encoding and the other arguments, are inputs of the task
    @get:Internal val options: CompileOptions,
) : CommandLineArgumentProvider {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    val moduleInfo: FileTree = javaSources.matching { it.include("**/$MODULE_INFO_JAVA") }

    override fun asArguments(): Iterable<String> {
        // A missing directory, when compileKotlin had no sources, fails javac with '-Xlint:path -Werror'
        val kotlinClassesDir = kotlinClasses.files.singleOrNull()?.takeIf { it.isDirectory } ?: return emptyList()
        // javac already reads its own destination directory, where some builds put the Kotlin classes too
        if (kotlinClassesDir == javaClassesDir.get().asFile) return emptyList()
        val moduleInfoFile = moduleInfo.files.singleOrNull() ?: return emptyList()
        val charset = options.encoding?.let { Charset.forName(it) } ?: Charset.defaultCharset()
        val moduleName = parseJavaModuleName(moduleInfoFile.readText(charset)) ?: run {
            logger.warn(
                "Could not read the module name from $moduleInfoFile, so no $PATCH_MODULE is passed to javac for the Kotlin classes"
            )
            return emptyList()
        }
        // javac fails when a module is patched more than once, so a --patch-module set up in the build takes precedence
        if (isModulePatched(moduleName, otherCompilerArguments())) return emptyList()
        return listOf(PATCH_MODULE, "$moduleName=${kotlinClassesDir.absolutePath}")
    }

    // Groovy build scripts can add GString values, so every argument is converted without a cast to String
    private fun otherCompilerArguments(): List<String> =
        (options.compilerArgs + options.compilerArgumentProviders.filter { it !== this }.flatMap { it.asArguments() })
            .map { argument: Any? -> argument.toString() }
            .flatMap { argument -> argumentFileArguments(argument) ?: listOf(argument) }

    // javac reads more arguments from an '@file' argument
    private fun argumentFileArguments(argument: String): List<String>? =
        File(argument.removePrefix("@")).takeIf { argument.startsWith("@") && it.isFile }
            ?.readText()?.split(whitespace)?.map { it.trim('"', '\'') }?.filter { it.isNotEmpty() }

    companion object {
        private const val MODULE_INFO_JAVA = "module-info.java"
        private const val PATCH_MODULE = "--patch-module"

        private val logger = Logging.getLogger(KotlinClassesPatchModuleArgumentProvider::class.java)

        private val whitespace = Regex("""\s+""")

        // For a module-info.java that javac accepts, the name read is javac's: Unicode escapes are translated first (JLS 3.3),
        // then comments and literals are removed (JLS 3.7, 3.10). After that, no import or annotation element value (JLS 9.7.1)
        // can contain a 'module' followed by a name and '{'.
        // A backslash starts a Unicode escape only after an even number of backslashes, as in javac
        private val unicodeEscape = Regex("""(?<!\\)((?:\\\\)*)\\u+([0-9a-fA-F]{4})""")
        private val literalsAndComments = Regex(
            """"{3}(?:\\.|[^\\])*?"{3}|"(?:\\.|[^"\\\r\n])*"|'(?:\\.|[^'\\\r\n])*'|//[^\r\n]*|/\*.*?\*/""",
            RegexOption.DOT_MATCHES_ALL
        )
        private val moduleDeclaration =
            Regex("""(?<![\p{javaJavaIdentifierPart}.])module\s+([\p{javaJavaIdentifierPart}.\s]+?)\s*\{""")
        private val moduleName = Regex("""\p{javaJavaIdentifierStart}\p{javaJavaIdentifierPart}*(?:\.\p{javaJavaIdentifierStart}\p{javaJavaIdentifierPart}*)*""")

        internal fun parseJavaModuleName(moduleInfoText: String): String? =
            moduleDeclaration.find(
                moduleInfoText
                    .replace(unicodeEscape) { it.groupValues[1] + it.groupValues[2].toInt(16).toChar() }
                    .replace(literalsAndComments, " ")
            )
                ?.groupValues?.get(1)
                // javac drops identifier-ignorable characters from names
                ?.filterNot { it.isWhitespace() || Character.isIdentifierIgnorable(it) }
                ?.takeIf { moduleName.matches(it) }

        internal fun isModulePatched(moduleName: String, arguments: List<String>): Boolean =
            arguments.withIndex().any { (index, argument) ->
                argument.startsWith("$PATCH_MODULE=$moduleName=") ||
                        argument == PATCH_MODULE && arguments.getOrNull(index + 1)?.startsWith("$moduleName=") == true
            }
    }
}
