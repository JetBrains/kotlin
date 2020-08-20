// !RENDER_TAGS: PROJECT, SETTINGS

package foo

import kotlin.test.Test

class <lineMarker descr="Run Test" project="", settings="cleanJvmTest jvmTest --tests \"foo.Foo\"">Foo</lineMarker> {
    @Test
    fun <lineMarker descr="Run Test" project="", settings="cleanJvmTest jvmTest --tests \"foo.Foo.test\"">test</lineMarker>() {
    }
}
