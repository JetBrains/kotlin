package repro

import repro.processor.Trigger

@Target(AnnotationTarget.TYPE)
annotation class PluginFunction

@Trigger
interface TestInterface {
    fun test(block: @PluginFunction () -> Unit)

    fun testList(blocks: List<@PluginFunction () -> Unit>)

    fun testReturn(): @PluginFunction () -> Unit

    fun testListReturn(): List<@PluginFunction () -> Unit>
}

class TestClass {
    val directProperty: @PluginFunction () -> Unit = {}

    val listProperty: List<@PluginFunction () -> Unit> = emptyList()

    var mutableDirectProperty: @PluginFunction () -> Unit = {}

    var mutableListProperty: List<@PluginFunction () -> Unit> = emptyList()
}
