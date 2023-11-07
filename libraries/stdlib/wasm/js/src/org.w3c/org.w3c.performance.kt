/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE") // TODO: Fix in dukat: https://github.com/Kotlin/dukat/issues/124

package org.w3c.performance

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.events.*

/**
 * Exposes the JavaScript [Performance](https://developer.mozilla.org/en/docs/Web/API/Performance) to Kotlin
 */
public external abstract class Performance : EventTarget, JsAny {
    open val timing: PerformanceTiming
    open val navigation: PerformanceNavigation
    fun now(): Double
}

public external interface GlobalPerformance : JsAny {
    val performance: Performance
}

/**
 * Exposes the JavaScript [PerformanceTiming](https://developer.mozilla.org/en/docs/Web/API/PerformanceTiming) to Kotlin
 */
public external abstract class PerformanceTiming : JsAny {
    open val navigationStart: JsNumber
    open val unloadEventStart: JsNumber
    open val unloadEventEnd: JsNumber
    open val redirectStart: JsNumber
    open val redirectEnd: JsNumber
    open val fetchStart: JsNumber
    open val domainLookupStart: JsNumber
    open val domainLookupEnd: JsNumber
    open val connectStart: JsNumber
    open val connectEnd: JsNumber
    open val secureConnectionStart: JsNumber
    open val requestStart: JsNumber
    open val responseStart: JsNumber
    open val responseEnd: JsNumber
    open val domLoading: JsNumber
    open val domInteractive: JsNumber
    open val domContentLoadedEventStart: JsNumber
    open val domContentLoadedEventEnd: JsNumber
    open val domComplete: JsNumber
    open val loadEventStart: JsNumber
    open val loadEventEnd: JsNumber
}

/**
 * Exposes the JavaScript [PerformanceNavigation](https://developer.mozilla.org/en/docs/Web/API/PerformanceNavigation) to Kotlin
 */
public external abstract class PerformanceNavigation : JsAny {
    open val type: Short
    open val redirectCount: Short

    companion object {
        val TYPE_NAVIGATE: Short
        val TYPE_RELOAD: Short
        val TYPE_BACK_FORWARD: Short
        val TYPE_RESERVED: Short
    }
}