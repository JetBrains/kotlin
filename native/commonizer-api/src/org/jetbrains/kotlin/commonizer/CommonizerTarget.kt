/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable

// N.B. TargetPlatform/SimplePlatform are non exhaustive enough to address both target platforms such as
// JVM, JS and concrete Kotlin/Native targets, e.g. macos_x64, ios_x64, linux_x64.
public sealed class CommonizerTarget : Serializable {
    final override fun toString(): String = identityString
}

public data class LeafCommonizerTarget public constructor(val name: String) : CommonizerTarget() {
    public constructor(konanTarget: KonanTarget) : this(konanTarget.name)

    public val konanTargetOrNull: KonanTarget? = KonanTarget.predefinedTargets[name]

    public val konanTarget: KonanTarget get() = konanTargetOrNull ?: error("Unknown KonanTarget: $name")
}

public data class SharedCommonizerTarget(val targets: Set<LeafCommonizerTarget>) : CommonizerTarget() {
    public constructor(vararg targets: LeafCommonizerTarget) : this(targets.toSet())
    public constructor(vararg targets: KonanTarget) : this(targets.toSet())
    public constructor(targets: Iterable<KonanTarget>) : this(targets.map(::LeafCommonizerTarget).toSet())
}

public fun CommonizerTarget(konanTargets: Iterable<KonanTarget>): CommonizerTarget {
    val konanTargetsSet = konanTargets.toSet()
    require(konanTargetsSet.isNotEmpty()) { "Empty set of of konanTargets" }
    val leafTargets = konanTargetsSet.map(::LeafCommonizerTarget)
    return leafTargets.singleOrNull() ?: SharedCommonizerTarget(leafTargets.toSet())
}

public fun CommonizerTarget(konanTarget: KonanTarget): LeafCommonizerTarget {
    return LeafCommonizerTarget(konanTarget)
}

public fun CommonizerTarget(konanTarget: KonanTarget, vararg konanTargets: KonanTarget): SharedCommonizerTarget {
    val targets = ArrayList<KonanTarget>(konanTargets.size + 1).apply {
        add(konanTarget)
        addAll(konanTargets)
    }
    return SharedCommonizerTarget(targets.map(::LeafCommonizerTarget).toSet())
}

public fun CommonizerTarget(
    commonizerTarget: LeafCommonizerTarget,
    vararg commonizerTargets: LeafCommonizerTarget
): SharedCommonizerTarget {
    val targets = mutableListOf<LeafCommonizerTarget>().apply {
        add(commonizerTarget)
        addAll(commonizerTargets)
    }
    return SharedCommonizerTarget(targets.toSet())
}

public val CommonizerTarget.identityString: String
    get() = when (this) {
        is LeafCommonizerTarget -> name
        is SharedCommonizerTarget -> identityString
    }

private val SharedCommonizerTarget.identityString: String
    get() {
        val segments = targets.map(CommonizerTarget::identityString).sorted()
        return segments.joinToString(
            separator = ", ", prefix = "(", postfix = ")"
        )
    }

public val CommonizerTarget.konanTargets: Set<KonanTarget>
    get() {
        return when (this) {
            is LeafCommonizerTarget -> setOf(konanTarget)
            is SharedCommonizerTarget -> targets.flatMap { it.konanTargets }.toSet()
        }
    }

public val Iterable<CommonizerTarget>.konanTargets: Set<KonanTarget> get() = flatMapTo(mutableSetOf()) { it.konanTargets }

// REMOVE
public val CommonizerTarget.level: Int
    get() {
        return when (this) {
            is LeafCommonizerTarget -> return 0
            is SharedCommonizerTarget -> if (targets.isNotEmpty()) targets.maxOf { it.level } + 1 else 0
        }
    }


public fun CommonizerTarget.allLeaves(): Set<LeafCommonizerTarget> {
    return when (this) {
        is LeafCommonizerTarget -> setOf(this)
        is SharedCommonizerTarget -> this.targets
    }
}

public fun Iterable<CommonizerTarget>.allLeaves(): Set<LeafCommonizerTarget> {
    return flatMapTo(mutableSetOf()) { target -> target.allLeaves() }
}

public fun CommonizerTarget.withAllLeaves(): Set<CommonizerTarget> {
    return when (this) {
        is LeafCommonizerTarget -> setOf(this)
        is SharedCommonizerTarget -> setOf(this) + targets
    }
}

public fun Iterable<CommonizerTarget>.withAllLeaves(): Set<CommonizerTarget> {
    return flatMapTo(mutableSetOf()) { it.withAllLeaves() }
}