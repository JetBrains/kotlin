/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.name.FqName

class ScopedKotlinNameBuilder {
    private var scope: NameScope? = null

    fun complete(name: KotlinName): ScopedKotlinName {
        require(scope != null) { "No scope specified "}
        return ScopedKotlinName(scope!!, name)
    }

    fun packageScope(packageName: FqName) {
        require (scope == null) { "Invalid scope combination: package after $scope" }
        scope = PackageScope(packageName)
    }

    fun packageScope(packageName: List<String>) {
        require (scope == null) { "Invalid scope combination: package after $scope" }
        scope = PackageScope(FqName.fromSegments(packageName))
    }

    fun classScope(className: ClassKotlinName) {
        require (scope != null) { "Class scope cannot be top-level" }
        scope = ClassScope(scope!!, className)
    }

    fun publicScope() {
        require(scope is ClassScope) { "Public scope must be in a class scope." }
        scope = PublicScope(scope!!)
    }

    fun privateScope() {
        require(scope is ClassScope) { "Private scope must be in a class scope." }
        scope = PrivateScope(scope!!)
    }

    fun parameterScope() {
        require(scope == null) { "Parameter scope cannot be nested." }
        scope = ParameterScope
    }

    fun localScope(level: Int) {
        require (scope == null) { "Local scope cannot be nested." }
        scope = LocalScope(level)
    }
}

// TODO: generalise this to work for all names.
fun buildName(init: ScopedKotlinNameBuilder.() -> KotlinName): ScopedKotlinName =
    ScopedKotlinNameBuilder().run { complete(init()) }