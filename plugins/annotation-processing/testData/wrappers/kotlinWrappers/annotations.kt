// FQNAME: test.Main

package test

import kotlin.reflect.KClass

@MainAnno("A", 5)
class Main {
    @field:XAnno(color = Color.GREEN)
    val x: String = ""
    
    @get:YAnno(arrayOf<String>("Mary", "Tom"), intArrayOf(1, 3, 5), arrayOf<Color>(Color.GREEN, Color.RED))
    val y: String = ""
    
    @set:ZAnno(String::class, arrayOf(String::class, Long::class, Main::class))
    var z: String = ""

    // Property annotations are lost here (we don't create Elements (javac API) for the synthetic propertyName$annotations() methods)
    @MainAnno("B", 6)
    val zz: String = ""
}

enum class Color {
    RED, GREEN, BLUE
}

annotation class MainAnno(val a: String, val b: Int)

annotation class XAnno(val color: Color)

annotation class YAnno(val names: Array<String>, val ints: IntArray, val colors: Array<Color>)

annotation class ZAnno(val clazz: KClass<*>, val classes: Array<KClass<*>>)