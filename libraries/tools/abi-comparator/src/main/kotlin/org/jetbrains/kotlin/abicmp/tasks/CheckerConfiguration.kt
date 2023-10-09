package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.checkers.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

val allClassCheckers = listOf(
    classPropertyChecker(ClassNode::version),
    classPropertyChecker(ClassNode::access) { v -> "${v.toString(2)} ${v.classFlags()}" },
    classPropertyChecker("internalName", ClassNode::name),
    classPropertyChecker(ClassNode::signature),
    classPropertyChecker("superClassInternalName", ClassNode::superName),
    classPropertyChecker("superInterfaces") { it.interfaces.cast<List<String>>().sorted() },
    classPropertyChecker(ClassNode::sourceFile),
    classPropertyChecker(ClassNode::outerClass),
    classPropertyChecker(ClassNode::outerMethod),
    classPropertyChecker(ClassNode::outerMethodDesc),
    ClassAnnotationsChecker(ClassNode::visibleAnnotations),
    ClassAnnotationsChecker(ClassNode::invisibleAnnotations),
    KotlinMetadataChecker(),
    InnerClassesListChecker(),
    MethodsListChecker(),
    FieldsListChecker()
)

val allMethodCheckers = listOf(
    methodPropertyChecker(MethodNode::access) { v -> "${v.toString(2)} ${v.methodFlags()}" },
    methodPropertyChecker("methodName", MethodNode::name),
    methodPropertyChecker(MethodNode::desc),
    methodPropertyChecker(MethodNode::signature, ignoreOnEquallyInvisibleMethods = true),
    methodPropertyChecker("exceptions") { it.exceptions.listOfNotNull<String>().sorted() },
    methodPropertyChecker("annotationDefault") { it.annotationDefault?.toAnnotationArgumentValue() },
    MethodAnnotationsChecker(MethodNode::visibleAnnotations),
    MethodAnnotationsChecker(MethodNode::invisibleAnnotations),
    MethodParameterAnnotationsChecker(MethodNode::visibleParameterAnnotations),
    MethodParameterAnnotationsChecker(MethodNode::invisibleParameterAnnotations)
)

val allFieldCheckers = listOf(
    fieldPropertyChecker(FieldNode::access) { v -> "${v.toString(2)} ${v.fieldFlags()}" },
    fieldPropertyChecker("fieldName", FieldNode::name),
    fieldPropertyChecker(FieldNode::desc),
    fieldPropertyChecker(FieldNode::signature),
    fieldPropertyChecker("initialValue", FieldNode::value),
    FieldAnnotationsChecker(FieldNode::visibleAnnotations),
    FieldAnnotationsChecker(FieldNode::invisibleAnnotations, ignoreNullabilityAnnotationsInIrBuild = true)
)

class CheckerConfigurationBuilder {
    private val enabledExclusively = HashSet<String>()
    private val disabled = HashSet<String>()

    fun enableExclusively(name: String) {
        enabledExclusively.add(name)
    }

    fun disable(name: String) {
        disabled.add(name)
    }

    fun build() = CheckerConfiguration(enabledExclusively, disabled)
}

inline fun checkerConfiguration(b: CheckerConfigurationBuilder.() -> Unit): CheckerConfiguration {
    val builder = CheckerConfigurationBuilder()
    builder.b()
    return builder.build()
}

class CheckerConfiguration(private val enabledExclusively: Set<String>, private val disabled: Set<String>) {

    val enabledClassCheckers: List<ClassChecker> =
        allClassCheckers.filter { it.isEnabled() }

    val enabledMethodCheckers: List<MethodChecker> =
        allMethodCheckers.filter { it.isEnabled() }

    val enabledFieldCheckers: List<FieldChecker> =
        allFieldCheckers.filter { it.isEnabled() }

    val enabledCheckers: List<Checker>
        get() = enabledClassCheckers + enabledMethodCheckers + enabledFieldCheckers

    private fun Checker.isEnabled(): Boolean {
        if (enabledExclusively.isNotEmpty() && name !in enabledExclusively) return false
        return name !in disabled
    }
}