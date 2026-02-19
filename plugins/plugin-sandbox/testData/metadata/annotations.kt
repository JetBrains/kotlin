// LANGUAGE: +AnnotationsInMetadata

package test

import org.jetbrains.kotlin.plugin.sandbox.AddAnnotations

@AddAnnotations
class Some(val x: Int) {
    fun foo() {}

    class Derived
}
