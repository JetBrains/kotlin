package org.jetbrains.kotlin.testGenerator.model

import org.jetbrains.kotlin.test.MuteExtraSuffix

interface TSuite {
    val abstractTestClass: Class<*>
    val generatedClassName: String

    val models: List<TModel>
    val annotations: List<TAnnotation>
    val imports: List<String>
}

interface MutableTSuite : TSuite {
    override val models: MutableList<TModel>
    override val annotations: MutableList<TAnnotation>
    override val imports: MutableList<String>
}

class TSuiteImpl(override val abstractTestClass: Class<*>, override val generatedClassName: String) : MutableTSuite {
    override val models = mutableListOf<TModel>()
    override val annotations = mutableListOf<TAnnotation>()
    override val imports = mutableListOf<String>()
}

inline fun <reified T: Any> MutableTGroup.suite(
    generatedClassName: String = getDefaultSuiteTestClassName(T::class.java),
    block: MutableTSuite.() -> Unit
) {
    suites += TSuiteImpl(T::class.java, generatedClassName).apply(block)
}

@PublishedApi
internal fun getDefaultSuiteTestClassName(clazz: Class<*>): String {
    val packageName = clazz.`package`.name
    val simpleName = clazz.simpleName

    require(simpleName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $simpleName" }
    return packageName + '.' + simpleName.substringAfter("Abstract") + "Generated"
}

fun MutableTSuite.muteExtraSuffix(suffix: String) {
    annotations += TAnnotation<MuteExtraSuffix>(suffix)
    imports += MuteExtraSuffix::class.java.name
}