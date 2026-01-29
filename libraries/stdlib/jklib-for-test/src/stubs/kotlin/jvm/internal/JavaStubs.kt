package kotlin.jvm.internal

import kotlin.reflect.KClass
import kotlin.reflect.KDeclarationContainer
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty2
import kotlin.reflect.KMutableProperty2


open class PropertyReference0 : CallableReference() {
    open fun get(): Any? = null
    override fun getOwner(): KDeclarationContainer = TODO()
    override val name: String get() = ""
    override val signature: String get() = ""
}

open class MutablePropertyReference0 : PropertyReference0() {
    open fun set(value: Any?): Unit = Unit
}

open class PropertyReference1 : CallableReference()
open class MutablePropertyReference1 : PropertyReference1()
open class PropertyReference2 : CallableReference()
open class MutablePropertyReference2 : PropertyReference2()

open class CallableReference : KDeclarationContainer {
    override val members: Collection<kotlin.reflect.KCallable<*>> get() = emptyList()
    open val name: String get() = ""
    open val signature: String get() = ""
    open fun getOwner(): KDeclarationContainer = this
}

object Reflection {
    const val REFLECTION_NOT_AVAILABLE = "Reflection is not available"

    fun getOrCreateKotlinClass(javaClass: Class<*>): KClass<*> = TODO()

    fun renderLambdaToString(lambda: Lambda<*>): String = "Stub"
    fun renderLambdaToString(function: FunctionBase<*>): String = "Stub"
    
    // fun function(f: FunctionReference): KFunction<*> = TODO() 
    fun property0(p: PropertyReference0): KProperty0<*> = TODO()
    fun mutableProperty0(p: MutablePropertyReference0): KMutableProperty0<*> = TODO()
    fun property1(p: PropertyReference1): KProperty1<*, *> = TODO()
    fun mutableProperty1(p: MutablePropertyReference1): KMutableProperty1<*, *> = TODO()
    fun property2(p: PropertyReference2): KProperty2<*, *, *> = TODO()
    fun mutableProperty2(p: MutablePropertyReference2): KMutableProperty2<*, *, *> = TODO()
}


