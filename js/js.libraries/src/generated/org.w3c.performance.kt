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

native public interface Performance {
    val timing: PerformanceTiming
        get() = noImpl
    val navigation: PerformanceNavigation
        get() = noImpl
    fun now(): Double = noImpl
}

native public interface PerformanceTiming {
    val navigationStart: Long
        get() = noImpl
    val unloadEventStart: Long
        get() = noImpl
    val unloadEventEnd: Long
        get() = noImpl
    val redirectStart: Long
        get() = noImpl
    val redirectEnd: Long
        get() = noImpl
    val fetchStart: Long
        get() = noImpl
    val domainLookupStart: Long
        get() = noImpl
    val domainLookupEnd: Long
        get() = noImpl
    val connectStart: Long
        get() = noImpl
    val connectEnd: Long
        get() = noImpl
    val secureConnectionStart: Long
        get() = noImpl
    val requestStart: Long
        get() = noImpl
    val responseStart: Long
        get() = noImpl
    val responseEnd: Long
        get() = noImpl
    val domLoading: Long
        get() = noImpl
    val domInteractive: Long
        get() = noImpl
    val domContentLoadedEventStart: Long
        get() = noImpl
    val domContentLoadedEventEnd: Long
        get() = noImpl
    val domComplete: Long
        get() = noImpl
    val loadEventStart: Long
        get() = noImpl
    val loadEventEnd: Long
        get() = noImpl
}

native public interface PerformanceNavigation {
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

