import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import java.io.Serializable
import javax.inject.Inject

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class TestInputsCheckExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val isNative: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val allowFlightRecorder: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    /**
     * Directories that remain declared Gradle inputs, but should be granted as whole directories
     * in the generated security policy instead of one permission per contained file.
     */
    val coarseInputDirectories: ConfigurableFileCollection = objects.fileCollection()
    val extraPermissions: ListProperty<String> = objects.listProperty(String::class.java)
}

class TestInputsCheckCacheSpec(
    private val inputsCheckIsSupported: Boolean,
    private val testInputsCheckEnabled: Provider<Boolean>,
) : Spec<Task>, Serializable {
    override fun isSatisfiedBy(element: Task): Boolean =
        !inputsCheckIsSupported || !testInputsCheckEnabled.get()
}

fun Test.testInputsCheck(action: Action<TestInputsCheckExtension>) {
    configure<TestInputsCheckExtension> {
        action.execute(this)
    }
}
