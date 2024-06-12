package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Interpreter<T> {
    val expectedArguments: List<ExpectedArgument>

    data class ExpectedArgument(
        val name: String,
        val klass: KType,
        val lens: Lens,
        val defaultValue: DefaultValue<*>
    )

    sealed interface Lens

    data object Value : Lens

    data object ReturnType : Lens

    data object Dsl : Lens

    data object Schema : Lens

    data object  Id : Lens

    // required to compute whether resulting schema should be inheritor of previous class or a new class
    fun startingSchema(arguments: Map<String, Success<Any?>>, kotlinTypeFacade: KotlinTypeFacade): PluginDataFrameSchema?

    fun interpret(arguments: Map<String, Success<Any?>>, kotlinTypeFacade: KotlinTypeFacade): InterpretationResult<T>

    sealed interface InterpretationResult<out T>

    class Success<out T>(val value: T) : InterpretationResult<T> {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success<*>

            return value == other.value
        }

        override fun hashCode(): Int {
            return value?.hashCode() ?: 0
        }
    }

    class Error(val message: String?) : InterpretationResult<Nothing>
}

sealed interface DefaultValue<out T>

class Present<T>(val value: T) : DefaultValue<T>
data object Absent : DefaultValue<Nothing>

open class Arguments(private val arguments: Map<String, Interpreter.Success<Any?>>, kotlinTypeFacade: KotlinTypeFacade): KotlinTypeFacade by kotlinTypeFacade {
    operator fun get(s: String): Any? = (arguments[s] ?: error("")).value
    operator fun contains(key: String): Boolean {
        return arguments.contains(key)
    }
}

abstract class AbstractInterpreter<T> : Interpreter<T> {
    @PublishedApi
    internal val _expectedArguments: MutableList<Interpreter.ExpectedArgument> = mutableListOf()

    override val expectedArguments: List<Interpreter.ExpectedArgument> = _expectedArguments

    protected open val Arguments.startingSchema: PluginDataFrameSchema? get() = null

    final override fun startingSchema(arguments: Map<String, Interpreter.Success<Any?>>, kotlinTypeFacade: KotlinTypeFacade): PluginDataFrameSchema? {
        return Arguments(arguments, kotlinTypeFacade).startingSchema
    }

    inline fun <Value, reified CompileTimeValue> argConvert(
        defaultValue: DefaultValue<Value> = Absent,
        name: ArgumentName? = null,
        lens: Interpreter.Lens = Interpreter.Value,
        crossinline converter: (CompileTimeValue) -> Value
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Arguments, Value>> = PropertyDelegateProvider { thisRef: Any?, property ->
        val name = name?.value ?: property.name
        _expectedArguments.add(Interpreter.ExpectedArgument(name, typeOf<CompileTimeValue>(), lens, defaultValue))
        ReadOnlyProperty { args, _ ->
            if (name !in args && defaultValue is Present) {
                defaultValue.value
            } else {
                converter(args[name] as CompileTimeValue)
            }
        }
    }

    fun <Value> arg(
        name: ArgumentName? = null,
        expectedType: KType? = null,
        defaultValue: DefaultValue<Value> = Absent,
        lens: Interpreter.Lens = Interpreter.Value
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Arguments, Value>> = PropertyDelegateProvider { thisRef: Any?, property ->
        val name = name?.value ?: property.name
        _expectedArguments.add(
            Interpreter.ExpectedArgument(
                name,
                expectedType ?: property.returnType,
                lens,
                defaultValue
            )
        )
        ReadOnlyProperty { args, _ ->
            if (name !in args && defaultValue is Present) {
                defaultValue.value
            } else {
                @Suppress("UNCHECKED_CAST")
                args[name] as Value
            }
        }
    }

    class ArgumentName private constructor(val value: String) {
        companion object {
            fun of(name: String): ArgumentName = ArgumentName(name)
        }
    }

    fun name(name: String): ArgumentName = ArgumentName.of(name)

    final override fun interpret(arguments: Map<String, Interpreter.Success<Any?>>, kotlinTypeFacade: KotlinTypeFacade): Interpreter.InterpretationResult<T> {
        return try {
            Arguments(arguments, kotlinTypeFacade).interpret().let { Interpreter.Success(it) }
        } catch (e: Exception) {
            Interpreter.Error(e.message + e.stackTrace.contentToString())
        }
    }

    abstract fun Arguments.interpret(): T
}

interface SchemaModificationInterpreter : Interpreter<PluginDataFrameSchema> {

    override fun interpret(arguments: Map<String, Interpreter.Success<Any?>>, kotlinTypeFacade: KotlinTypeFacade): Interpreter.InterpretationResult<PluginDataFrameSchema>
}

abstract class AbstractSchemaModificationInterpreter :
    AbstractInterpreter<PluginDataFrameSchema>(),
    SchemaModificationInterpreter
