/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv.targets

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.Serializable


interface BcvTargetSpec : Serializable {

    /** Enables or disables API generation and validation for this target */
    val enabled: Property<Boolean>

    /**
     * The classes to generate signatures for.
     *
     * Note that if [inputJar] has a value, the contents of [inputClasses] will be ignored
     */
    val inputClasses: ConfigurableFileCollection

    /**
     * A JAR that contains the classes to generate signatures for.
     *
     * Note that if [inputJar] has a value, the contents of [inputClasses] will be ignored
     */
    val inputJar: RegularFileProperty

    /**
     * Fully qualified names of annotations that can be used to explicitly mark public declarations.
     *
     * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
     * all declarations not covered by any of them will be considered non-public.
     * [ignoredPackages], [ignoredClasses] and [ignoredMarkers] can be used for additional filtering.
     */
    val publicMarkers: SetProperty<String>

    /**
     * Fully qualified package names that contain public declarations.
     *
     * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
     * all declarations not covered by any of them will be considered non-public.
     *
     * [ignoredPackages], [ignoredClasses] and [ignoredMarkers] can be used for additional filtering.
     */
    val publicPackages: SetProperty<String>

    /**
     * Fully qualified names of public classes.
     *
     * If at least one of [publicMarkers], [publicPackages] or [publicClasses] is defined,
     * all declarations not covered by any of them will be considered non-public.
     *
     * [ignoredPackages], [ignoredClasses] and [ignoredMarkers] can be used for additional filtering.
     */
    val publicClasses: SetProperty<String>

    /**
     * Fully qualified names of annotations that effectively exclude declarations from being public.
     * Example of such annotation could be `kotlinx.coroutines.InternalCoroutinesApi`.
     */
    val ignoredMarkers: SetProperty<String>

    /**
     * Fully qualified package names that are not considered public API.
     * For example, it could be `kotlinx.coroutines.internal` or `kotlinx.serialization.implementation`.
     */
    val ignoredPackages: SetProperty<String>

    /**
     * Fully qualified names of classes that are ignored by the API check.
     * Example of such a class could be `com.package.android.BuildConfig`.
     */
    val ignoredClasses: SetProperty<String>
}
