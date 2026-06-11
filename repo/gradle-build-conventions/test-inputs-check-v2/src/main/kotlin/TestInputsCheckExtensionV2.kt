import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class TestInputsCheckExtensionV2 @Inject constructor(objects: ObjectFactory) {
    val failFast: Property<Boolean> = objects.property<Boolean>()
        .convention(false)
}
