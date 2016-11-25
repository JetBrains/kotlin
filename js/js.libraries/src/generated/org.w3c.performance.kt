/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.performance

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

public external abstract class Performance : EventTarget() {
    open val timing: PerformanceTiming
        get() = noImpl
    open val navigation: PerformanceNavigation
        get() = noImpl
    fun now(): Double = noImpl
}

public external interface GlobalPerformance {
    val performance: Performance
        get() = noImpl
}

public external abstract class PerformanceTiming {
    open val navigationStart: Int
        get() = noImpl
    open val unloadEventStart: Int
        get() = noImpl
    open val unloadEventEnd: Int
        get() = noImpl
    open val redirectStart: Int
        get() = noImpl
    open val redirectEnd: Int
        get() = noImpl
    open val fetchStart: Int
        get() = noImpl
    open val domainLookupStart: Int
        get() = noImpl
    open val domainLookupEnd: Int
        get() = noImpl
    open val connectStart: Int
        get() = noImpl
    open val connectEnd: Int
        get() = noImpl
    open val secureConnectionStart: Int
        get() = noImpl
    open val requestStart: Int
        get() = noImpl
    open val responseStart: Int
        get() = noImpl
    open val responseEnd: Int
        get() = noImpl
    open val domLoading: Int
        get() = noImpl
    open val domInteractive: Int
        get() = noImpl
    open val domContentLoadedEventStart: Int
        get() = noImpl
    open val domContentLoadedEventEnd: Int
        get() = noImpl
    open val domComplete: Int
        get() = noImpl
    open val loadEventStart: Int
        get() = noImpl
    open val loadEventEnd: Int
        get() = noImpl
}

public external abstract class PerformanceNavigation {
    open val type: Short
        get() = noImpl
    open val redirectCount: Short
        get() = noImpl

    companion object {
        val TYPE_NAVIGATE: Short = 0
        val TYPE_RELOAD: Short = 1
        val TYPE_BACK_FORWARD: Short = 2
        val TYPE_RESERVED: Short = 255
    }
}

