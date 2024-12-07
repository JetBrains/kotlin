/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testhelpers

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.slf4j.Marker

class StubLogger(
    private val name: String
) : Logger {
    override fun getName(): String = name

    override fun isTraceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun trace(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(message: String?, vararg objects: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun debug(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun info(message: String?, vararg objects: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun info(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    var isWarnEnabledFlag = true
    override fun isWarnEnabled(): Boolean = isWarnEnabledFlag
    override fun isWarnEnabled(marker: Marker?): Boolean = isWarnEnabledFlag

    val loggedWarnings = mutableListOf<String>()

    override fun warn(msg: String) {
        loggedWarnings.add(msg)
    }

    override fun warn(format: String, arg: Any) {
        loggedWarnings.add(format.format(arg))
    }

    override fun warn(format: String, vararg arguments: Any) {
        loggedWarnings.add(format.format(arguments))
    }

    override fun warn(format: String, arg1: Any, arg2: Any) {
        loggedWarnings.add(format.format(arg1, arg2))
    }

    override fun warn(msg: String, t: Throwable) {
        loggedWarnings.add(msg)
    }

    override fun warn(marker: Marker, msg: String) {
        loggedWarnings.add(msg)
    }

    override fun warn(marker: Marker, format: String, arg: Any) {
        loggedWarnings.add(format.format(arg))
    }

    override fun warn(marker: Marker, format: String, arg1: Any, arg2: Any) {
        loggedWarnings.add(format.format(arg1, arg2))
    }

    override fun warn(marker: Marker, format: String, vararg arguments: Any) {
        loggedWarnings.add(format.format(arguments))
    }

    override fun warn(marker: Marker, msg: String, t: Throwable) {
        loggedWarnings.add(msg)
    }

    override fun isErrorEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun error(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isLifecycleEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun lifecycle(message: String?) {
        TODO("Not yet implemented")
    }

    override fun lifecycle(message: String?, vararg objects: Any?) {
        TODO("Not yet implemented")
    }

    override fun lifecycle(message: String?, throwable: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isQuietEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun quiet(message: String?) {
        TODO("Not yet implemented")
    }

    override fun quiet(message: String?, vararg objects: Any?) {
        TODO("Not yet implemented")
    }

    override fun quiet(message: String?, throwable: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isEnabled(level: LogLevel?): Boolean {
        TODO("Not yet implemented")
    }

    override fun log(level: LogLevel?, message: String?) {
        TODO("Not yet implemented")
    }

    override fun log(level: LogLevel?, message: String?, vararg objects: Any?) {
        TODO("Not yet implemented")
    }

    override fun log(level: LogLevel?, message: String?, throwable: Throwable?) {
        TODO("Not yet implemented")
    }
}