/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.CommandLineArgumentProvider

abstract class JfrArgumentProvider : CommandLineArgumentProvider {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jfcFile: RegularFileProperty

    @get:Internal
    abstract val jfrFile: ConfigurableFileCollection

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    override fun asArguments(): Iterable<String> =
        buildList {
            add(
                "-XX:StartFlightRecording:" +
                        "settings=${jfcFile.get().asFile.absolutePath}," +
                        "filename=${jfrFile.singleFile.absolutePath}," +
                        "disk=true," +
                        "dumponexit=true"
            )
            if (javaLauncher.get().metadata.languageVersion.asInt() > 8) {
                // required to emit JFR events from the instrumented JDK classes (like FileInputStream)
                addAll(listOf("--add-reads", "java.base=jdk.jfr"))
            }
        }
}
