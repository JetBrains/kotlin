/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.performance

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.workers.*
import org.w3c.xhr.*

/**
 * Exposes the JavaScript [Performance](https://developer.mozilla.org/en/docs/Web/API/Performance) to Kotlin
 */
public external abstract class Performance : EventTarget {
    open val timing: PerformanceTiming
    open val navigation: PerformanceNavigation
    fun now(): Double
}

public external interface GlobalPerformance {
    val performance: Performance
}

/**
 * Exposes the JavaScript [PerformanceTiming](https://developer.mozilla.org/en/docs/Web/API/PerformanceTiming) to Kotlin
 */
public external abstract class PerformanceTiming {
    open val navigationStart: Int
    open val unloadEventStart: Int
    open val unloadEventEnd: Int
    open val redirectStart: Int
    open val redirectEnd: Int
    open val fetchStart: Int
    open val domainLookupStart: Int
    open val domainLookupEnd: Int
    open val connectStart: Int
    open val connectEnd: Int
    open val secureConnectionStart: Int
    open val requestStart: Int
    open val responseStart: Int
    open val responseEnd: Int
    open val domLoading: Int
    open val domInteractive: Int
    open val domContentLoadedEventStart: Int
    open val domContentLoadedEventEnd: Int
    open val domComplete: Int
    open val loadEventStart: Int
    open val loadEventEnd: Int
}

/**
 * Exposes the JavaScript [PerformanceNavigation](https://developer.mozilla.org/en/docs/Web/API/PerformanceNavigation) to Kotlin
 */
public external abstract class PerformanceNavigation {
    open val type: Short
    open val redirectCount: Short

    companion object {
        val TYPE_NAVIGATE: Short
        val TYPE_RELOAD: Short
        val TYPE_BACK_FORWARD: Short
        val TYPE_RESERVED: Short
    }
}

