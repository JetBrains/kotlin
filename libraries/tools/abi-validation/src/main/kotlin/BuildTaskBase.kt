/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import java.io.File

public abstract class BuildTaskBase : DefaultTask() {
    private val extension = project.apiValidationExtensionOrNull

    @OutputFile
    public lateinit var outputApiFile: File

    private var _ignoredPackages: Set<String>? = null
    @get:Input
    public var ignoredPackages : Set<String>
        get() = _ignoredPackages ?: extension?.ignoredPackages ?: emptySet()
        set(value) { _ignoredPackages = value }

    private var _nonPublicMarkes: Set<String>? = null
    @get:Input
    public var nonPublicMarkers : Set<String>
        get() = _nonPublicMarkes ?: extension?.nonPublicMarkers ?: emptySet()
        set(value) { _nonPublicMarkes = value }

    private var _ignoredClasses: Set<String>? = null
    @get:Input
    public var ignoredClasses : Set<String>
        get() = _ignoredClasses ?: extension?.ignoredClasses ?: emptySet()
        set(value) { _ignoredClasses = value }

    private var _publicPackages: Set<String>? = null
    @get:Input
    public var publicPackages: Set<String>
        get() = _publicPackages ?: extension?.publicPackages ?: emptySet()
        set(value) { _publicPackages = value }

    private var _publicMarkers: Set<String>? = null
    @get:Input
    public var publicMarkers: Set<String>
        get() = _publicMarkers ?: extension?.publicMarkers ?: emptySet()
        set(value) { _publicMarkers = value}

    private var _publicClasses: Set<String>? = null
    @get:Input
    public var publicClasses: Set<String>
        get() = _publicClasses ?: extension?.publicClasses ?: emptySet()
        set(value) { _publicClasses = value }

    @get:Internal
    internal val projectName = project.name
}
