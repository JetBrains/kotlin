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
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public trait Performance {
    var timing: PerformanceTiming
        get() = noImpl
        set(value) = noImpl
    var navigation: PerformanceNavigation
        get() = noImpl
        set(value) = noImpl
    fun now(): Double = noImpl
}

native public trait PerformanceTiming {
    var navigationStart: Long
        get() = noImpl
        set(value) = noImpl
    var unloadEventStart: Long
        get() = noImpl
        set(value) = noImpl
    var unloadEventEnd: Long
        get() = noImpl
        set(value) = noImpl
    var redirectStart: Long
        get() = noImpl
        set(value) = noImpl
    var redirectEnd: Long
        get() = noImpl
        set(value) = noImpl
    var fetchStart: Long
        get() = noImpl
        set(value) = noImpl
    var domainLookupStart: Long
        get() = noImpl
        set(value) = noImpl
    var domainLookupEnd: Long
        get() = noImpl
        set(value) = noImpl
    var connectStart: Long
        get() = noImpl
        set(value) = noImpl
    var connectEnd: Long
        get() = noImpl
        set(value) = noImpl
    var secureConnectionStart: Long
        get() = noImpl
        set(value) = noImpl
    var requestStart: Long
        get() = noImpl
        set(value) = noImpl
    var responseStart: Long
        get() = noImpl
        set(value) = noImpl
    var responseEnd: Long
        get() = noImpl
        set(value) = noImpl
    var domLoading: Long
        get() = noImpl
        set(value) = noImpl
    var domInteractive: Long
        get() = noImpl
        set(value) = noImpl
    var domContentLoadedEventStart: Long
        get() = noImpl
        set(value) = noImpl
    var domContentLoadedEventEnd: Long
        get() = noImpl
        set(value) = noImpl
    var domComplete: Long
        get() = noImpl
        set(value) = noImpl
    var loadEventStart: Long
        get() = noImpl
        set(value) = noImpl
    var loadEventEnd: Long
        get() = noImpl
        set(value) = noImpl
}

native public trait PerformanceNavigation {
    var type: Short
        get() = noImpl
        set(value) = noImpl
    var redirectCount: Short
        get() = noImpl
        set(value) = noImpl

    companion object {
        val TYPE_NAVIGATE: Short = 0
        val TYPE_RELOAD: Short = 1
        val TYPE_BACK_FORWARD: Short = 2
        val TYPE_RESERVED: Short = 255
    }
}

