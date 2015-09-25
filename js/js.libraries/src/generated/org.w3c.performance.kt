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

@native public interface Performance {
    val timing: PerformanceTiming
        get() = noImpl
    val navigation: PerformanceNavigation
        get() = noImpl
    fun now(): Double = noImpl
}

@native public interface PerformanceTiming {
    val navigationStart: Int
        get() = noImpl
    val unloadEventStart: Int
        get() = noImpl
    val unloadEventEnd: Int
        get() = noImpl
    val redirectStart: Int
        get() = noImpl
    val redirectEnd: Int
        get() = noImpl
    val fetchStart: Int
        get() = noImpl
    val domainLookupStart: Int
        get() = noImpl
    val domainLookupEnd: Int
        get() = noImpl
    val connectStart: Int
        get() = noImpl
    val connectEnd: Int
        get() = noImpl
    val secureConnectionStart: Int
        get() = noImpl
    val requestStart: Int
        get() = noImpl
    val responseStart: Int
        get() = noImpl
    val responseEnd: Int
        get() = noImpl
    val domLoading: Int
        get() = noImpl
    val domInteractive: Int
        get() = noImpl
    val domContentLoadedEventStart: Int
        get() = noImpl
    val domContentLoadedEventEnd: Int
        get() = noImpl
    val domComplete: Int
        get() = noImpl
    val loadEventStart: Int
        get() = noImpl
    val loadEventEnd: Int
        get() = noImpl
}

@native public interface PerformanceNavigation {
    val type: Short
        get() = noImpl
    val redirectCount: Short
        get() = noImpl

    companion object {
        val TYPE_NAVIGATE: Short = 0
        val TYPE_RELOAD: Short = 1
        val TYPE_BACK_FORWARD: Short = 2
        val TYPE_RESERVED: Short = 255
    }
}

