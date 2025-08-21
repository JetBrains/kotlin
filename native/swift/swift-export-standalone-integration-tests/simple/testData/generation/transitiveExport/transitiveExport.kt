// KIND: STANDALONE
// MODULE: state
// FILE: inner_state.kt
package oh.my.state.inner

class InnerState(val bytes: ByteArray? = null)

// FILE: state.kt
package oh.my.state

import oh.my.state.inner.*

class State(val innerState: InnerState? = null)

class ExtractedByTypealias
typealias ToExtract = ExtractedByTypealias

// MODULE: feature
// FILE: features.kt
package oh.my.kotlin

class FeatureA {}

class FeatureB

// MODULE: anotherFeature(state)
// FILE: main.kt

class FeatureC() {
    val state: oh.my.state.State = TODO()

    fun baz(): oh.my.state.ToExtract = TODO()
}

// MODULE: main(feature,anotherFeature)
// EXPORT_TO_SWIFT
// FILE: main.kt
import oh.my.kotlin.FeatureA

fun foo(): FeatureA {
    return FeatureA()
}

fun bar(): FeatureC {
    return FeatureC()
}

// MODULE: main_2(feature)
// EXPORT_TO_SWIFT
// FILE: main_2.kt
package foo

import oh.my.kotlin.FeatureB

fun foo(): FeatureB {
    return FeatureB()
}

fun bar(): Array<String> {
    return TODO()
}
