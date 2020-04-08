package sample

import kotlin.test.Test
import kotlin.test.assertTrue

object <lineMarker descr="Run Test" settings=" cleanJsBrowserTest jsBrowserTest --tests \"sample.Obj\" cleanJsNodeTest jsNodeTest --tests \"sample.Obj\" cleanJvmTest jvmTest --tests \"sample.Obj\" --continue">Obj</lineMarker> {
    @Test
    fun <lineMarker descr="Run Test" settings=" cleanJsBrowserTest jsBrowserTest --tests \"sample.Obj.myTest\" cleanJsNodeTest jsNodeTest --tests \"sample.Obj.myTest\" cleanJvmTest jvmTest --tests \"sample.Obj.myTest\" --continue">myTest</lineMarker>() {}
}