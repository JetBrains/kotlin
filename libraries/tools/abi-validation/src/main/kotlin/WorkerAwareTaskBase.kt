/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath

public abstract class WorkerAwareTaskBase : DefaultTask() {
    @get:Classpath
    public abstract val runtimeClasspath: ConfigurableFileCollection
}
