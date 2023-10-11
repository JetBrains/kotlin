package org.jetbrains.kotlin.gradle.idea

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinCompilationCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.closure


sealed interface IdeaKotlinFragment : HasMutableExtras {
    val fragmentName: String

    val sourceDirs: IdeaKotlinClasspath
    val resourceDirs: IdeaKotlinClasspath

    val refinesFragments: Set<String>
    val refinesFragmentsClosure: Set<String>

    val refinedByFragments: Set<String>
    val refinedByFragmentsClosure: Set<String>

    val compilations: Set<IdeaKotlinCompilationCoordinates>

    val dependencies: Set<IdeaKotlinDependency>
}

fun IdeaKotlinFragment(
    coordinates: IdeaKotlinSourceCoordinates,
    fragmentName: String,
    sourceDirs: IdeaKotlinClasspath,
    resourceDirs: IdeaKotlinClasspath,
    dependencies: List<IdeaKotlinDependency>,
    compilations: List<IdeaKotlinCompilationCoordinates>,
    extras: MutableExtras,
): IdeaKotlinFragment = IdeaKotlinFragmentImpl(
    fragmentName = fragmentName,
    sourceDirs = sourceDirs,
    resourceDirs = resourceDirs,
    dependencies = dependencies,
    compilations = compilations,
    extras = extras
)

private data class IdeaKotlinFragmentImpl(
    override val fragmentName: String,
    override val sourceDirs: IdeaKotlinClasspath,
    override val resourceDirs: IdeaKotlinClasspath,
    override val dependencies: List<IdeaKotlinDependency>,
    override val compilations: List<IdeaKotlinCompilationCoordinates>,
    override val extras: MutableExtras,
) : IdeaKotlinFragment


class RefinesGraph(
    private val refinesEdges: Map<String, Set<String>>,
) {

    private val refinedByEdges =

    inner class Fragment(val fragmentName: String) {
        val refinesFragments: Set<String> get() = refinesEdges[fragmentName].orEmpty()
        val refinesEdgesClosure: Set<String> get() = fragmentName.closure { refinesEdges[it].orEmpty() }
        val refinedByEdges
    }
}