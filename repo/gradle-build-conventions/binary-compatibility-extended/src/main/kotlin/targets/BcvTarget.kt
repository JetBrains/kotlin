/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv.targets

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.io.Serializable
import javax.inject.Inject

abstract class BcvTarget
@Inject
constructor(
    /**
     * The JVM platform being targeted.
     *
     * Targets with the same [platformType] will be grouped together into a single API declaration.
     */
    @get:Input
    val platformType: String,
) : BcvTargetSpec, Serializable, Named {

    @get:Input
    @get:Optional
    abstract override val enabled: Property<Boolean>

    @get:Classpath
    abstract override val inputClasses: ConfigurableFileCollection

    @get:Classpath
    @get:Optional
    abstract override val inputJar: RegularFileProperty

    /** @see org.jetbrains.kotlin.build.bcv.targets.BcvTarget.publicMarkers */
    @get:Input
    @get:Optional
    abstract override val publicMarkers: SetProperty<String>

    /** @see org.jetbrains.kotlin.build.bcv.targets.BcvTarget.publicPackages */
    @get:Input
    @get:Optional
    abstract override val publicPackages: SetProperty<String>

    /** @see org.jetbrains.kotlin.build.bcv.targets.BcvTarget.publicClasses */
    @get:Input
    @get:Optional
    abstract override val publicClasses: SetProperty<String>

    /** @see org.jetbrains.kotlin.build.bcv.targets.BcvTarget.ignoredMarkers */
    @get:Input
    @get:Optional
    abstract override val ignoredMarkers: SetProperty<String>

    /** @see org.jetbrains.kotlin.build.bcv.targets.BcvTarget.ignoredPackages */
    @get:Input
    @get:Optional
    abstract override val ignoredPackages: SetProperty<String>

    /** @see org.jetbrains.kotlin.build.bcv.targets.BcvTarget.ignoredClasses */
    @get:Input
    @get:Optional
    abstract override val ignoredClasses: SetProperty<String>

    @Internal
    override fun getName(): String = platformType
}
