/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

internal interface Named<T : Named<T>> {
    val name: String
}

internal class TestKpmGradleProject(override val name: String) : Named<TestKpmGradleProject> {
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
        val module = modules.getOrPut(name) { TestKpmModule(this, name) }
        configure(module)
        return module
    }

    fun moduleNamed(name: String): TestKpmModule =
        modules[name] ?: error("Module with name $name doesn't exist. Existing modules: $modules")

    val main get() = moduleNamed("main")
    val test get() = moduleNamed("test")

    override fun toString(): String = ":$name"
}

internal class TestKpmModule(
    val kpmGradleProject: TestKpmGradleProject,
    override val name: String
) : Named<TestKpmModule> {
    var publicationMode: KpmModulePublicationMode = KpmModulePublicationMode.PRIVATE

    val fragments = ConfigurableSet<TestKpmFragment>().apply {
        add(TestKpmFragment(this@TestKpmModule, "common"))
    }

    fun fragment(name: String, kind: FragmentKind, configure: TestKpmFragment.() -> Unit = { }): TestKpmFragment {
        val result = fragments.getOrPut(name) { TestKpmFragment(this, name, kind) }
        require(result.kind == kind) { "There's already a fragment with the name $name and kind $kind" }
        return result.also(configure)
    }

    fun fragmentNamed(name: String): TestKpmFragment =
        fragments[name] ?: error("Fragment with name $name doesn't exist. Existing fragments $fragments")

    fun depends(otherModule: TestKpmModule, kind: TestDependencyKind) {
        fragmentNamed("common").depends(otherModule, kind)
    }

    fun makePublic(publicationMode: KpmModulePublicationMode) {
        this.publicationMode = publicationMode
    }

    companion object {
        const val MAIN_NAME = "main"
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
    override val name: String,
    val kind: FragmentKind = FragmentKind.COMMON_FRAGMENT,
) : Named<TestKpmFragment> {
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
internal class ConfigurableSet<T : Named<T>> private constructor(
    private val _items: MutableMap<String, T>
) : Collection<T> by _items.values {
    constructor() : this(mutableMapOf())

    private val allItemsActions = mutableListOf<T.() -> Unit>()

    fun add(item: T) {
        allItemsActions.forEach { action -> action(item) }
        _items[item.name] = item
    }

    fun withAll(action: T.() -> Unit) {
        _items.values.forEach(action)
        allItemsActions.add(action)
    }

    fun getOrPut(name: String, defaultValue: () -> T): T = _items.getOrPut(name, defaultValue)

    operator fun get(name: String): T? = _items[name]
    operator fun set(name: String, value: T) {
        _items[name] = value
    }
}