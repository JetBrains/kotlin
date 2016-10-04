package org.jetbrains.kotlin.annotation.processing.test.processor

enum class RGBColors { RED, GREEN, BLUE }
annotation class ColorsAnnotation(val colors: Array<RGBColors>)

@ColorsAnnotation(colors = arrayOf(RGBColors.BLUE, RGBColors.RED))
class Test