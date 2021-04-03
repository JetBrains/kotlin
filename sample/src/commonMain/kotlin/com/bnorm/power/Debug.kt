package com.bnorm.power

val debugLog = StringBuilder()

fun <T> dbg(value: T): T = value

fun <T> dbg(value: T, msg: String): T {
  debugLog.appendLine(msg)
  return value
}
