/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import TestCompilePaths.KOTLIN_ANNOTATIONS_PATH
import TestCompilePaths.KOTLIN_COMMON_STDLIB_PATH
import TestCompilePaths.KOTLIN_FULL_STDLIB_PATH
import TestCompilePaths.KOTLIN_JS_KOTLIN_TEST_KLIB_PATH
import TestCompilePaths.KOTLIN_JS_REDUCED_STDLIB_PATH
import TestCompilePaths.KOTLIN_JS_STDLIB_KLIB_PATH
import TestCompilePaths.KOTLIN_MINIMAL_STDLIB_PATH
import TestCompilePaths.KOTLIN_MOCKJDKMODIFIED_RUNTIME_PATH
import TestCompilePaths.KOTLIN_MOCKJDK_ANNOTATIONS_PATH
import TestCompilePaths.KOTLIN_MOCKJDK_RUNTIME_PATH
import TestCompilePaths.KOTLIN_REFLECT_JAR_PATH
import TestCompilePaths.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH
import TestCompilePaths.KOTLIN_SCRIPT_RUNTIME_PATH
import TestCompilePaths.KOTLIN_TESTDATA_ROOTS
import TestCompilePaths.KOTLIN_TEST_JAR_PATH
import TestCompilePaths.KOTLIN_THIRDPARTY_ANNOTATIONS_PATH
import TestCompilePaths.KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH
import TestCompilePaths.KOTLIN_THIRDPARTY_JAVA9_ANNOTATIONS_PATH
import TestCompilePaths.KOTLIN_THIRDPARTY_JSR305_PATH
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider

abstract class TestCompilerRuntimeArgumentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val stdlibRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibMinimalRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinReflectJarForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibCommonRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val scriptRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinTestJarForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinAnnotationsForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val scriptingPluginForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibJsRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val testJsRuntimeForTests: ConfigurableFileCollection

    @get:Input
    abstract val testDataMap: MapProperty<String, String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testDataFiles: ListProperty<Directory>

    @get:InputFile
    @get:Classpath
    @get:Optional
    abstract val mockJdkRuntimeJar: RegularFileProperty

    @get:InputFile
    @get:Classpath
    @get:Optional
    abstract val mockJdkRuntime: RegularFileProperty

    @get:InputFile
    @get:Classpath
    @get:Optional
    abstract val mockJDKModifiedRuntime: RegularFileProperty

    @get:InputFile
    @get:Classpath
    @get:Optional
    abstract val mockJdkAnnotationsJar: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val thirdPartyAnnotations: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val thirdPartyJava8Annotations: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val thirdPartyJava9Annotations: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val thirdPartyJsr305: DirectoryProperty

    private fun ifNotEmpty(property: String, fileCollection: ConfigurableFileCollection): String? =
        if (fileCollection.isEmpty) null else argument(property, fileCollection)

    private fun argument(property: String, fileCollection: ConfigurableFileCollection) =
        "-D$property=${fileCollection.joinToString(",") { it.absolutePath }}"

    override fun asArguments(): Iterable<String> {
        return listOfNotNull(
            argument(KOTLIN_FULL_STDLIB_PATH, stdlibRuntimeForTests),
            argument(KOTLIN_MINIMAL_STDLIB_PATH, stdlibMinimalRuntimeForTests),
            argument(KOTLIN_REFLECT_JAR_PATH, kotlinReflectJarForTests),
            ifNotEmpty(KOTLIN_COMMON_STDLIB_PATH, stdlibCommonRuntimeForTests),
            ifNotEmpty(KOTLIN_SCRIPT_RUNTIME_PATH, scriptRuntimeForTests),
            ifNotEmpty(KOTLIN_TEST_JAR_PATH, kotlinTestJarForTests),
            ifNotEmpty(KOTLIN_ANNOTATIONS_PATH, kotlinAnnotationsForTests),
            ifNotEmpty(KOTLIN_SCRIPTING_PLUGIN_CLASSPATH, scriptingPluginForTests),
            ifNotEmpty(KOTLIN_JS_STDLIB_KLIB_PATH, stdlibJsRuntimeForTests),
            ifNotEmpty(KOTLIN_JS_REDUCED_STDLIB_PATH, stdlibJsRuntimeForTests),
            ifNotEmpty(KOTLIN_JS_KOTLIN_TEST_KLIB_PATH, testJsRuntimeForTests),
            mockJdkRuntimeJar.orNull?.let { "-D$KOTLIN_MOCKJDK_RUNTIME_PATH=${it.asFile.absolutePath}" },
            mockJDKModifiedRuntime.orNull?.let { "-D$KOTLIN_MOCKJDKMODIFIED_RUNTIME_PATH=${it.asFile.absolutePath}" },
            mockJdkAnnotationsJar.orNull?.let { "-D$KOTLIN_MOCKJDK_ANNOTATIONS_PATH=${it.asFile.absolutePath}" },
            thirdPartyAnnotations.orNull?.let { "-D$KOTLIN_THIRDPARTY_ANNOTATIONS_PATH=${it.asFile.absolutePath}" },
            thirdPartyJava8Annotations.orNull?.let { "-D$KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH=${it.asFile.absolutePath}" },
            thirdPartyJava9Annotations.orNull?.let { "-D$KOTLIN_THIRDPARTY_JAVA9_ANNOTATIONS_PATH=${it.asFile.absolutePath}" },
            thirdPartyJsr305.orNull?.let { "-D$KOTLIN_THIRDPARTY_JSR305_PATH=${it.asFile.absolutePath}" },
            testDataMap.get().takeIf { it.isNotEmpty() }
                ?.map { it.key + "=" + it.value }
                ?.joinToString(prefix = "-D$KOTLIN_TESTDATA_ROOTS=", separator = ";"),
        )
    }
}
