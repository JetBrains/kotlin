/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import java.io.File
import kotlin.reflect.KProperty

// Workaround for https://github.com/gradle/gradle/issues/12388
// which should be fixed via https://github.com/gradle/gradle/issues/24767
internal fun <IN : Any, OUT> Provider<IN>.mapOrNull(
    providerFactory: ProviderFactory,
    block: (IN) -> OUT?
): Provider<OUT> = flatMap { providerFactory.provider { (block(it)) } }

internal operator fun <T> Provider<T>.getValue(thisRef: Any?, property: KProperty<*>) = get()

internal operator fun <T> Property<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    set(value)
}

internal fun <T : Any> Project.newProperty(initialize: (() -> T)? = null): Property<T> =
    @Suppress("UNCHECKED_CAST")
    (project.objects.property(Any::class.java) as Property<T>).apply {
        if (initialize != null)
            set(provider(initialize))
    }

internal inline fun <reified T : Any?> ObjectFactory.property() = property(T::class.java)

internal inline fun <reified T : Any?> ObjectFactory.listProperty() = listProperty(T::class.java)

internal inline fun <reified T : Any?> ObjectFactory.setProperty() = setProperty(T::class.java)

internal inline fun <reified T : Any?> ObjectFactory.property(initialValue: T) = property<T>().value(initialValue)

internal inline fun <reified T : Any?> ObjectFactory.property(initialValue: Provider<T>) = property<T>().value(initialValue)

internal inline fun <reified T : Any?> ObjectFactory.setPropertyWithValue(
    initialValue: Provider<Iterable<T>>
) = setProperty<T>().value(initialValue)

internal inline fun <reified T : Any?> ObjectFactory.setPropertyWithValue(
    initialValue: Iterable<T>
) = setProperty<T>().value(initialValue)

internal inline fun <reified T : Any?> ObjectFactory.setPropertyWithLazyValue(
    noinline lazyValue: () -> Iterable<T>
) = setPropertyWithValue(providerWithLazyConvention(lazyValue))

internal inline fun <reified T : Any?> ObjectFactory.propertyWithConvention(
    conventionValue: Provider<T>
) = property<T>().convention(conventionValue)

internal inline fun <reified T : Any?> ObjectFactory.propertyWithConvention(
    conventionValue: T
) = property<T>().convention(conventionValue)

internal inline fun <reified T : Any?> ObjectFactory.listPropertyWithConvention(
    conventionValue: Iterable<T>
) = listProperty<T>().convention(conventionValue)

internal inline fun <reified T : Any?> ObjectFactory.providerWithLazyConvention(
    noinline lazyConventionValue: () -> T
) = property(lazyConventionValue).map { it.invoke() }

internal inline fun <reified T : Any> ObjectFactory.newInstance() = newInstance(T::class.java)

internal inline fun <reified T : Any> ObjectFactory.newInstance(vararg parameters: Any) =
    newInstance(T::class.java, *parameters)

internal inline fun <reified T : Any> ObjectFactory.propertyWithNewInstance(
    vararg parameters: Any
) = propertyWithConvention(newInstance(T::class.java, *parameters))

internal fun <PropType : Any?, T : Property<PropType>> T.chainedFinalizeValueOnRead(): T =
    apply {
        finalizeValueOnRead()
    }

internal fun <PropType : Any?, T : ListProperty<PropType>> T.chainedFinalizeValueOnRead(): T =
    apply {
        finalizeValueOnRead()
    }

internal fun <PropType : Any?, T : Property<PropType>> T.chainedFinalizeValue(): T =
    apply {
        finalizeValue()
    }

internal fun <PropType : Any?, T : Property<PropType>> T.chainedDisallowChanges(): T =
    apply {
        disallowChanges()
    }

internal fun <PropType : Any?, T : SetProperty<PropType>> T.chainedDisallowChanges(): T =
    apply {
        disallowChanges()
    }


// Before 5.0 fileProperty is created via ProjectLayout
// https://docs.gradle.org/current/javadoc/org/gradle/api/model/ObjectFactory.html#fileProperty--
internal fun Project.newFileProperty(initialize: (() -> File)? = null): RegularFileProperty {
    val regularFileProperty = project.objects.fileProperty()

    return regularFileProperty.apply {
        if (initialize != null) {
            set(project.layout.file(project.provider(initialize)))
        }
    }
}

internal fun ObjectFactory.fileProperty(initialValue: File): RegularFileProperty = fileProperty()
    .apply { set(initialValue) }

internal fun ObjectFactory.directoryProperty(initialValue: File): DirectoryProperty = directoryProperty()
    .apply { set(initialValue) }

internal fun Project.filesProvider(
    vararg buildDependencies: Any,
    provider: () -> Any?
): ConfigurableFileCollection {
    return project.files(provider).builtBy(*buildDependencies)
}

internal fun <T : Task> T.outputFilesProvider(provider: T.() -> Any): ConfigurableFileCollection {
    return project.filesProvider(this) { provider() }
}

internal inline fun <reified T> Project.listProperty(noinline itemsProvider: () -> Iterable<T>): ListProperty<T> =
    objects.listProperty(T::class.java).apply { set(provider(itemsProvider)) }

internal inline fun <reified T> Project.setProperty(noinline itemsProvider: () -> Iterable<T>): SetProperty<T> =
    objects.setProperty(T::class.java).apply { set(provider(itemsProvider)) }

/**
 * Changing Provider will be evaluated every time it accessed.
 *
 * And its producing [code] will be serilalised to Configuration Cache as is
 * So that it still will be evaluated during Task Execution phase.
 * It is very convenient for Configuration Cache compatibility.
 *
 * It is recommended to use Task Output's and map/flatMap them to other Task Inputs but in cases when TaskOutput's is not available
 * as Gradle's Properties or Providers then this [changing] provider can be used.
 *
 * name `changing` and overall concept is borrowed from Gradle internal API [org.gradle.api.internal.provider.Providers.changing]
 *
 * @see org.gradle.api.internal.provider.ChangingProvider
 */
internal fun <T> ProviderFactory.changing(code: () -> T): Provider<T> {
    @Suppress("UNCHECKED_CAST")
    val adhocValueSourceClass = AdhocValueSource::class.java as Class<AdhocValueSource<T>>
    return of(adhocValueSourceClass) { valueSourceSpec ->
        valueSourceSpec.parameters {
            it.producingLambda.set(code)
        }
    }
}

private abstract class AdhocValueSource<T> : ValueSource<T, AdhocValueSource.Parameters> {

    // this interface can't be parameterized with T since it breaks internal Gradle logic
    // This is why producingLambda is '() -> Any?' but not '() -> T'
    interface Parameters : ValueSourceParameters {
        val producingLambda: Property<() -> Any?>
    }

    override fun obtain(): T {
        @Suppress("UNCHECKED_CAST")
        return parameters.producingLambda.get().invoke() as T
    }
}

internal fun Provider<Directory>.dir(path: String): Provider<Directory> = map { it.dir(path) }