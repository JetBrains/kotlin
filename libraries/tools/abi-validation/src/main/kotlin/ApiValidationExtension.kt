/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
}
