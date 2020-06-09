package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSTargetedTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "targeted"

    fun testTargetAnyField() {
        doTest(
            """
                class '_Class {  
                    val 'Field+ = '_Init?
                }
            """.trimIndent()
        )
    }

    fun testTargetSpecificField() {
        doTest(
            """
                class '_Class {  
                    val 'Field+ = 45
                }
            """.trimIndent()
        )
    }
}