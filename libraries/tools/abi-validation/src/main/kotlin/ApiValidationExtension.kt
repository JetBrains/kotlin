/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

open class ApiValidationExtension {

    /**
     * Disables API validation checks completely.
     */
    public var validationDisabled = false

    /**
     * Fully qualified package names that are not consider public API.
     * For example, it could be `kotlinx.coroutines.internal` or `kotlinx.serialization.implementation`.
     */
    public var ignoredPackages: MutableSet<String> = HashSet()

    /**
     * Projects that are ignored by the API check.
     */
    public var ignoredProjects: MutableSet<String> = HashSet()

    /**
     * Fully qualified names of annotations that effectively exclude declarations from being public.
     * Example of such annotation could be `kotlinx.coroutines.InternalCoroutinesApi`.
     */
    public var nonPublicMarkers: MutableSet<String> = HashSet()

    /**
     * Fully qualified names of classes that are ignored by the API check.
     * Example of such a class could be `com.package.android.BuildConfig`.
     */
    public var ignoredClasses: MutableSet<String> = HashSet()
}
