/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See libraries/tools/idl2k for details

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.geolocation

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.css.masking.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.mediacapture.*
import org.w3c.dom.parsing.*
import org.w3c.dom.pointerevents.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

/**
 * Exposes the JavaScript [Geolocation](https://developer.mozilla.org/en/docs/Web/API/Geolocation) to Kotlin
 */
public external interface Geolocation {
    fun getCurrentPosition(successCallback: (Position) -> Unit, errorCallback: (PositionError) -> Unit = definedExternally, options: PositionOptions = definedExternally): Unit
    fun watchPosition(successCallback: (Position) -> Unit, errorCallback: (PositionError) -> Unit = definedExternally, options: PositionOptions = definedExternally): Int
    fun clearWatch(watchId: Int): Unit
}

/**
 * Exposes the JavaScript [PositionOptions](https://developer.mozilla.org/en/docs/Web/API/PositionOptions) to Kotlin
 */
public external interface PositionOptions {
    var enableHighAccuracy: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var timeout: Int? /* = definedExternally */
        get() = definedExternally
        set(value) = definedExternally
    var maximumAge: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun PositionOptions(enableHighAccuracy: Boolean? = false, timeout: Int? = definedExternally, maximumAge: Int? = 0): PositionOptions {
    val o = js("({})")

    o["enableHighAccuracy"] = enableHighAccuracy
    o["timeout"] = timeout
    o["maximumAge"] = maximumAge

    return o
}

/**
 * Exposes the JavaScript [Position](https://developer.mozilla.org/en/docs/Web/API/Position) to Kotlin
 */
public external interface Position {
    val coords: Coordinates
    val timestamp: Number
}

/**
 * Exposes the JavaScript [Coordinates](https://developer.mozilla.org/en/docs/Web/API/Coordinates) to Kotlin
 */
public external interface Coordinates {
    val latitude: Double
    val longitude: Double
    val altitude: Double?
    val accuracy: Double
    val altitudeAccuracy: Double?
    val heading: Double?
    val speed: Double?
}

/**
 * Exposes the JavaScript [PositionError](https://developer.mozilla.org/en/docs/Web/API/PositionError) to Kotlin
 */
public external interface PositionError {
    val code: Short
    val message: String

    companion object {
        val PERMISSION_DENIED: Short
        val POSITION_UNAVAILABLE: Short
        val TIMEOUT: Short
    }
}

