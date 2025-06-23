import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class TestInputsCheckExtension @Inject constructor(objects: ObjectFactory){
    val isNative: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val useXcode: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}