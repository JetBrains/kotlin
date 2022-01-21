/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess

/**
 * The type of dependency for a particular [TestCompilation].
 *
 * [Library] - the [TestCompilation]s (modules) that should yield KLIBs to be consumed as dependency libraries in the current compilation.
 * [FriendLibrary] - similarly but friend modules (-friend-modules).
 * [IncludedLibrary] - similarly but included modules (-Xinclude).
 */
internal sealed interface TestCompilationDependencyType<A : TestCompilationArtifact> {
    object Library : TestCompilationDependencyType<TestCompilationArtifact.KLIB>
    object FriendLibrary : TestCompilationDependencyType<TestCompilationArtifact.KLIB>
    object IncludedLibrary : TestCompilationDependencyType<TestCompilationArtifact.KLIB>
}

internal sealed interface TestCompilationDependency<A : TestCompilationArtifact> {
    val artifact: A
    val type: TestCompilationDependencyType<A>
}

internal class CompiledDependency<A : TestCompilationArtifact>(
    val compilation: TestCompilation<A>,
    override val type: TestCompilationDependencyType<A>
) : TestCompilationDependency<A> {
    override val artifact: A get() = compilation.result.assertSuccess().resultingArtifact
}

internal class ExistingLibraryDependency(
    override val artifact: TestCompilationArtifact.KLIB,
    override val type: TestCompilationDependencyType<TestCompilationArtifact.KLIB>
) : TestCompilationDependency<TestCompilationArtifact.KLIB>
