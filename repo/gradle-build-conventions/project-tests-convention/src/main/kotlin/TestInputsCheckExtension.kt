import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import javax.inject.Inject

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class TestInputsCheckExtension @Inject constructor(objects: ObjectFactory){
    val isNative: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val allowFlightRecorder: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val extraPermissions: ListProperty<String> = objects.listProperty(String::class.java)
}

fun Test.testInputsCheck(action: Action<TestInputsCheckExtension>) {
    configure<TestInputsCheckExtension> {
        action.execute(this)
    }
}
