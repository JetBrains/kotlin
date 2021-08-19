/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

internal class TestKpmGradleProject(val name: String) {
    val modules: ConfigurableSet<TestKpmModule> = run {
        val main = TestKpmModule(this, "main").apply { publicationMode = KpmModulePublicationMode.STANDALONE }
        val test = TestKpmModule(this, "test").apply {
            fragmentNamed("common").moduleDependencies.add(TestKpmModuleDependency(main, TestDependencyKind.DIRECT))
        }
        ConfigurableSet<TestKpmModule>().apply {
            add(main)
            add(test)
        }
    }

    fun allModules(action: TestKpmModule.() -> Unit) {
        modules.withAll(action)
    }

    fun module(name: String, configure: TestKpmModule.() -> Unit): TestKpmModule {
        val module = modules.singleOrNull { it.name == name } ?: TestKpmModule(this, name).also(modules::add)
        configure(module)
        return module
    }

    fun moduleNamed(name: String) = modules.single { it.name == name }
    val main get() = moduleNamed("main")
    val test get() = moduleNamed("test")

    override fun toString(): String = ":$name"
}

internal class TestKpmModule(
    val kpmGradleProject: TestKpmGradleProject,
    val name: String
) {
    var publicationMode: KpmModulePublicationMode = KpmModulePublicationMode.PRIVATE

    val fragments = ConfigurableSet<TestKpmFragment>().apply {
        add(TestKpmFragment(this@TestKpmModule, "common"))
    }

    fun fragment(name: String, kind: FragmentKind, configure: TestKpmFragment.() -> Unit = { }): TestKpmFragment {
        val result = fragments.singleOrNull { it.name == name }
            ?.also { require(it.kind == kind) { "There's already a fragment with the name $name and kind $kind" } }
            ?: TestKpmFragment(this, name, kind).also(fragments::add)
        return result.also(configure)
    }

    fun fragmentNamed(name: String): TestKpmFragment = fragments.single { it.name == name }

    companion object {
        const val MAIN_NAME = "main"
    }

    fun depends(otherModule: TestKpmModule, kind: TestDependencyKind) {
        fragmentNamed("common").depends(otherModule, kind)
    }

    fun makePublic(publicationMode: KpmModulePublicationMode) {
        this.publicationMode = publicationMode
    }
}

internal enum class FragmentKind(val gradleType: String) {
    COMMON_FRAGMENT("KotlinGradleFragment"),
    JVM_VARIANT("KotlinJvmVariant"),
    LINUXX64_VARIANT("KotlinLinuxX64Variant"),
    IOSARM64_VARIANT("KotlinIosArm64Variant"),
    IOSX64_VARIANT("KotlinIosX64Variant")
}

internal class TestKpmFragment(
    val module: TestKpmModule,
    val name: String,
    val kind: FragmentKind = FragmentKind.COMMON_FRAGMENT,
) {
    val refines = mutableSetOf<TestKpmFragment>()
    val moduleDependencies = mutableSetOf<TestKpmModuleDependency>()

    fun refines(vararg otherFragments: TestKpmFragment) {
        require(otherFragments.all { it.module === module }) { "Only refinement within one module is supported" }
        refines.addAll(otherFragments)
    }

    val refinesClosure: Set<TestKpmFragment>
        get() = mutableSetOf<TestKpmFragment>().apply {
            fun visit(f: TestKpmFragment) {
                if (add(f)) f.refines.forEach(::visit)
            }
            visit(this@TestKpmFragment)
        }

    fun depends(otherModule: TestKpmModule, kind: TestDependencyKind) {
        if (otherModule.kpmGradleProject !== module.kpmGradleProject) {
            require(kind != TestDependencyKind.DIRECT) { "Direct dependencies are only allowed within one project" }
        }
        moduleDependencies.add(TestKpmModuleDependency(otherModule, kind))
    }

    private sealed class ExpectVisibilityItem {
        abstract val fragments: Iterable<TestKpmFragment>

        class ExpectVisibilityOf(override val fragments: Iterable<TestKpmFragment>) : ExpectVisibilityItem()
        class ExpectVisibilityOfLazy(val provideFragments: () -> Iterable<TestKpmFragment>) : ExpectVisibilityItem() {
            override val fragments: Iterable<TestKpmFragment> get() = provideFragments()
        }
    }

    private val _expectsVisibility = mutableListOf<ExpectVisibilityItem>()
    val expectsVisibility: Iterable<TestKpmFragment> get() = _expectsVisibility.flatMapTo(mutableSetOf(), ExpectVisibilityItem::fragments)

    fun expectVisibility(otherFragment: TestKpmFragment) {
        _expectsVisibility.add(ExpectVisibilityItem.ExpectVisibilityOf(listOf(otherFragment)))
    }

    fun expectVisibility(otherFragments: Iterable<TestKpmFragment>) {
        _expectsVisibility.add(ExpectVisibilityItem.ExpectVisibilityOf(otherFragments))
    }

    fun expectVisibility(provideOtherFragments: () -> Iterable<TestKpmFragment>) {
        _expectsVisibility.add(ExpectVisibilityItem.ExpectVisibilityOfLazy(provideOtherFragments))
    }
}

internal enum class KpmModulePublicationMode {
    PRIVATE, STANDALONE, EMBEDDED
}

internal enum class TestDependencyKind {
    DIRECT, PROJECT, PUBLISHED,
}

internal class TestKpmModuleDependency(val module: TestKpmModule, val dependencyKind: TestDependencyKind)
internal class ConfigurableSet<T> private constructor(val _items: MutableSet<T>) : Set<T> by _items {
    constructor() : this(mutableSetOf())

    private val allItemsActions = mutableListOf<T.() -> Unit>()

    fun add(item: T) {
        allItemsActions.forEach { action -> action(item) }
        _items.add(item)
    }

    fun withAll(action: T.() -> Unit) {
        _items.forEach(action)
        allItemsActions.add(action)
    }
}