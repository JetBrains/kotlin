/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.mergeBeans
import org.jetbrains.kotlin.compilerRunner.flattenCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import java.io.File

/**
 * Everything one JPS module contributes to a compilation, extracted from its build target.
 *
 * This is what `module.xml` used to carry per module. Unlike `module.xml` it describes exactly one module: one Build
 * Tools API operation cannot describe a chunk, which is why circular module dependencies are rejected at the seam.
 */
internal class JpsBtaCompilationUnit(
    val moduleName: String,
    val sources: List<File>,
    val outputDir: File,
    val classpath: Collection<File>,
    val friendDirs: Collection<File>,
    val javaSourceRoots: Collection<JvmSourceRoot>,
    val modularJdkRoot: File?,
    val incremental: JpsBtaIncrementalCompilation?,
)

/**
 * What the compiler needs to run incremental compilation itself, rather than JPS doing it.
 *
 * [sourcesChanges] describes only what JPS noticed; the compiler works out the rest of the compile set from
 * [JpsBtaCompilationUnit.sources], which is why that list stays complete even on an incremental build.
 *
 * @property workingDir where the compiler keeps its own incremental caches. Lives under the target's JPS data root so
 * that it is thrown away together with JPS's own data.
 * @property forceRecompilation JPS decided the target has to be rebuilt from scratch. Makes the compiler discard its
 * caches and clean [JpsBtaCompilationUnit.outputDir] and [workingDir] before compiling.
 */
internal class JpsBtaIncrementalCompilation(
    val workingDir: File,
    val sourcesChanges: SourcesChanges,
    val forceRecompilation: Boolean,
)

/**
 * Assembles one [K2JVMCompilerArguments] and flattens it to command line arguments.
 *
 * Deliberately not a hand-written mapping onto typed Build Tools API options: round-tripping through argument strings
 * carries everything the facet, the project settings and `additionalArguments` can express, so a new compiler argument
 * never needs a JPS change. This is what KGP does too, see `JvmBuildOperationFactory.createOperation`.
 *
 * The source list and the output directory stay out of the returned arguments: their command line forms (compiler free
 * arguments and `-d`) are restricted arguments in the Build Tools API and are parameters of
 * `jvmCompilationOperationBuilder` instead.
 */
internal fun JpsBtaCompilationUnit.toCompilerArgumentStrings(
    commonArguments: CommonCompilerArguments,
    moduleArguments: K2JVMCompilerArguments,
    compilerSettings: CompilerSettings,
): List<String> {
    val arguments = mergeBeans(commonArguments, XmlSerializerUtil.createCopy(moduleArguments))

    arguments.moduleName = moduleName
    arguments.classpath = classpath.joinToString(File.pathSeparator) { it.absolutePath }
    arguments.friendPaths = friendDirs.map { it.absolutePath }.toTypedArray()
    arguments.javaSourceRoots = javaSourceRoots.map { it.file.absolutePath }.toTypedArray()
    // `module.xml` carries a package prefix per Java source root, `-Xjava-package-prefix` is a single global value.
    // Irrelevant while only Kotlin-only modules are supported, a real constraint once mixed modules are.
    arguments.javaPackagePrefix = javaSourceRoots.firstNotNullOfOrNull { it.packagePrefix }
    arguments.reportOutputFiles = true

    // Implied by JPS: it puts the standard library on the classpath itself
    arguments.noStdlib = true
    arguments.noReflect = true

    // `module.xml` used to carry `modularJdkRoot` on a channel of its own, applied whatever `-no-jdk` said. Its only
    // command line form is `-jdk-home`, which `CompilerConfiguration.configureJdkHome` ignores outright when `-no-jdk`
    // is set, so the two cannot both be passed:
    //
    // - modular JDK: `-jdk-home` has to win, because it is the only way the JDK reaches the compiler at all. JPS puts
    //   the SDK on the classpath as `jrt://...!/java.base` URLs, which `findClassPathRoots` drops, and
    //   `CliJavaModuleFinder` derives the system modules from `jdkHome`. `-no-jdk` would leave `java.lang.*`
    //   unresolvable. Dropping it costs nothing: `configureJdkClasspathRoots` adds no roots for a modular JDK anyway.
    // - non-modular JDK: there is no `modularJdkRoot` and JPS supplies the JDK jars on the classpath itself, so
    //   `-no-jdk` has to stay, or `configureJdkHomeFromSystemProperty` would inject the build process's own JDK.
    arguments.jdkHome = modularJdkRoot?.absolutePath
    arguments.noJdk = modularJdkRoot == null

    // Restricted arguments in the Build Tools API, passed as operation parameters instead.
    // Nulling the destination after reading it mirrors KGP's own hack, tracked as KT-85394.
    arguments.destination = null
    arguments.freeArgs = emptyList()

    return flattenCompilerArguments(arguments, compilerSettings)
}
