/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

abstract class JfrArgumentProvider : CommandLineArgumentProvider {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jfcFile: RegularFileProperty

    @get:Internal
    abstract val jfrFile: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> =
        listOf(
            "-XX:StartFlightRecording:" +
                    "settings=${jfcFile.get().asFile.absolutePath}," +
                    "filename=${jfrFile.singleFile.absolutePath}," +
                    "disk=true," +
                    "dumponexit=true",
            // required to emit JFR events from instrumented JDK classes (like FileInputStream)
            "--add-reads", "java.base=jdk.jfr"
        )
}
