package org.kotlin.test

import org.kotlin.annotationProcessor.TestAnnotation
import java.awt.Color

@TestAnnotation
class SimpleClass

class Test(val a: String) {
    companion object {
        val a = Color.PINK
    }
}