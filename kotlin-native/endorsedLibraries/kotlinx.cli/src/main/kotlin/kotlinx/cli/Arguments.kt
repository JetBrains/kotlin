/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cli

import kotlin.reflect.KProperty

internal data class CLIEntityWrapper(var entity: CLIEntity<*>? = null)

/**
 * Base interface for all possible types of entities with default and required values.
 * Provides limitations for API that is accessible for different options/arguments types.
 * Allows to save the reason why option/argument can(or can't) be omitted in command line.
 *
 * @see [SingleOption], [MultipleOption], [SingleArgument], [MultipleArgument].
 */
interface DefaultRequiredType {
    /**
     * Type of an entity with default value.
     */
    class Default : DefaultRequiredType

    /**
     * Type of an entity which value should always be provided in command line.
     */
    class Required : DefaultRequiredType

    /**
     * Type of entity which is optional and has no default value.
     */
    class None : DefaultRequiredType
}

/**
 * The base class for a command line argument or an option.
 */
abstract class CLIEntity<TResult> internal constructor(val delegate: ArgumentValueDelegate<TResult>,
                                                       internal val owner: CLIEntityWrapper) {
    /**
     * The value of the option or argument parsed from command line.
     *
     * Accessing this property before it gets its value will result in an exception.
     * You can use [valueOrigin] property to find out whether the property has been already set.
     *
     * @see ArgumentValueDelegate.value
     */
    var value: TResult
        get() = delegate.value
        set(value) {
            check((delegate as ParsingValue<*, *>).valueOrigin != ArgParser.ValueOrigin.UNDEFINED) {
                "Resetting value of option/argument is only possible after parsing command line arguments." +
                        " ArgParser.parse(...) method should be called before"
            }
            delegate.value = value
        }

    /**
     * The origin of the option/argument value.
     */
    val valueOrigin: ArgParser.ValueOrigin
        get() = (delegate as ParsingValue<*, *>).valueOrigin

    private var delegateProvided = false

    /**
     * Returns the delegate object for property delegation and initializes it with the name of the delegated property.
     *
     * This operator makes it possible to delegate a property to this instance. It returns [delegate] object
     * to be used as an actual delegate and uses the name of the delegated property to initialize the full name
     * of the option/argument if it wasn't done during construction of that option/argument.
     *
     * @throws IllegalStateException in case of trying to use same delegate several times.
     */
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<TResult> {
        check(!delegateProvided) {
            "There is already used delegate for ${(delegate as ParsingValue<*, *>).descriptor.fullName}."
        }
        (delegate as ParsingValue<*, *>).provideName(prop.name)
        delegateProvided = true
        return delegate
    }
}

/**
 * The base class for command line arguments.
 *
 * You can use [ArgParser.argument] function to declare an argument.
 */
abstract class Argument<TResult> internal constructor(delegate: ArgumentValueDelegate<TResult>,
                                                      owner: CLIEntityWrapper): CLIEntity<TResult>(delegate, owner)

/**
 * The base class of an argument with a single value.
 *
 * A non-optional argument or an optional argument with a default value is represented with the [SingleArgument] inheritor.
 * An optional argument having nullable value is represented with the [SingleNullableArgument] inheritor.
 */
// TODO: investigate if we can collapse two inheritors into the single base class and specialize extensions by TResult upper bound
abstract class AbstractSingleArgument<T: Any, TResult, DefaultRequired: DefaultRequiredType> internal constructor(
    delegate: ArgumentValueDelegate<TResult>,
    owner: CLIEntityWrapper):
    Argument<TResult>(delegate, owner) {
    /**
     * Check descriptor for this kind of argument.
     */
    internal fun checkDescriptor(descriptor: ArgDescriptor<*, *>) {
        if (descriptor.number == null || descriptor.number > 1) {
            failAssertion("Argument with single value can't be initialized with descriptor for multiple values.")
        }
    }
}

/**
 * A non-optional argument or an optional argument with a default value.
 *
 * The [value] of such argument is non-null.
 */
class SingleArgument<T : Any, DefaultRequired: DefaultRequiredType> internal constructor(descriptor: ArgDescriptor<T, T>,
                                                   owner: CLIEntityWrapper):
    AbstractSingleArgument<T, T, DefaultRequired>(ArgumentSingleValue(descriptor), owner) {
    init {
        checkDescriptor(descriptor)
    }
}

/**
 * An optional argument with nullable [value].
 */
class SingleNullableArgument<T : Any> internal constructor(descriptor: ArgDescriptor<T, T>, owner: CLIEntityWrapper):
        AbstractSingleArgument<T, T?, DefaultRequiredType.None>(ArgumentSingleNullableValue(descriptor), owner) {
    init {
        checkDescriptor(descriptor)
    }
}

/**
 * An argument that allows several values to be provided in command line string.
 *
 * The [value] property of such argument has type `List<T>`.
 */
class MultipleArgument<T : Any, DefaultRequired: DefaultRequiredType> internal constructor(
    descriptor: ArgDescriptor<T, List<T>>, owner: CLIEntityWrapper):
        Argument<List<T>>(ArgumentMultipleValues(descriptor), owner) {
    init {
        if (descriptor.number != null && descriptor.number < 2) {
            failAssertion("Argument with multiple values can't be initialized with descriptor for single one.")
        }
    }
}

/**
 * Allows the argument to have several values specified in command line string.
 *
 * @param number the exact number of values expected for this argument, but at least 2.
 *
 * @throws IllegalArgumentException if number of values expected for this argument less than 2.
 */
fun <T : Any, TResult, DefaultRequired: DefaultRequiredType>
        AbstractSingleArgument<T, TResult, DefaultRequired>.multiple(number: Int): MultipleArgument<T, DefaultRequired> {
    require(number >= 2) { "multiple() modifier with value less than 2 is unavailable. It's already set to 1." }
    val newArgument = with(delegate.cast<ParsingValue<T, T>>().descriptor as ArgDescriptor) {
        MultipleArgument<T, DefaultRequired>(ArgDescriptor(type, fullName, number, description, listOfNotNull(defaultValue),
                required, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Allows the last argument to take all the trailing values in command line string.
 */
fun <T : Any, TResult, DefaultRequired: DefaultRequiredType> AbstractSingleArgument<T, TResult, DefaultRequired>.vararg():
        MultipleArgument<T, DefaultRequired> {
    val newArgument = with(delegate.cast<ParsingValue<T, T>>().descriptor as ArgDescriptor) {
        MultipleArgument<T, DefaultRequired>(ArgDescriptor(type, fullName, null, description, listOfNotNull(defaultValue),
                required, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Specifies the default value for the argument, that will be used when no value is provided for the argument
 * in command line string.
 *
 * Argument becomes optional, because value for it is set even if it isn't provided in command line.
 *
 * @param value the default value.
 */
fun <T: Any> SingleNullableArgument<T>.default(value: T): SingleArgument<T, DefaultRequiredType.Default> {
    val newArgument = with(delegate.cast<ParsingValue<T, T>>().descriptor as ArgDescriptor) {
        SingleArgument<T, DefaultRequiredType.Default>(ArgDescriptor(type, fullName, number, description, value,
            false, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Specifies the default value for the argument with multiple values, that will be used when no values are provided
 * for the argument in command line string.
 *
 * Argument becomes optional, because value for it is set even if it isn't provided in command line.
 *
 * @param value the default value, must be a non-empty collection.
 */
fun <T: Any> MultipleArgument<T, DefaultRequiredType.None>.default(value: Collection<T>):
        MultipleArgument<T, DefaultRequiredType.Default> {
    require (value.isNotEmpty()) { "Default value for argument can't be empty collection." }
    val newArgument = with(delegate.cast<ParsingValue<T, List<T>>>().descriptor as ArgDescriptor) {
        MultipleArgument<T, DefaultRequiredType.Default>(ArgDescriptor(type, fullName, number, description, value.toList(),
                required, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Allows the argument to have no value specified in command line string.
 *
 * The value of the argument is `null` in case if no value was specified in command line string.
 *
 * Note that only trailing arguments can be optional, i.e. no required arguments can follow optional ones.
 */
fun <T: Any> SingleArgument<T, DefaultRequiredType.Required>.optional(): SingleNullableArgument<T> {
    val newArgument = with(delegate.cast<ParsingValue<T, T>>().descriptor as ArgDescriptor) {
        SingleNullableArgument(ArgDescriptor(type, fullName, number, description, defaultValue,
                false, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

/**
 * Allows the argument with multiple values to have no values specified in command line string.
 *
 * The value of the argument is an empty list in case if no value was specified in command line string.
 *
 * Note that only trailing arguments can be optional: no required arguments can follow the optional ones.
 */
fun <T: Any> MultipleArgument<T, DefaultRequiredType.Required>.optional(): MultipleArgument<T, DefaultRequiredType.None> {
    val newArgument = with(delegate.cast<ParsingValue<T, List<T>>>().descriptor as ArgDescriptor) {
        MultipleArgument<T, DefaultRequiredType.None>(ArgDescriptor(type, fullName, number, description,
                defaultValue?.toList() ?: listOf(), false, deprecatedWarning), owner)
    }
    owner.entity = newArgument
    return newArgument
}

internal fun failAssertion(message: String): Nothing = throw AssertionError(message)

internal inline fun <reified T : Any> Any?.cast(): T = this as T