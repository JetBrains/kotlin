/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("RegexExtensionsJDK8Kt")
@file:kotlin.jvm.JvmPackageName("kotlin.text.jdk8")
package kotlin.text

/**
 * Returns a named group with the specified [name].
 *
 * @return An instance of [MatchGroup] if the group with the specified [name] was matched or `null` otherwise.
 * @throws IllegalArgumentException if there is no group with the specified [name] defined in the regex pattern.
 * @throws UnsupportedOperationException if this match group collection doesn't support getting match groups by name,
 * for example, when it's not supported by the current platform.
 */
@SinceKotlin("1.2")
public actual operator fun MatchGroupCollection.get(name: String): MatchGroup? {
    val namedGroups = this as? MatchNamedGroupCollection
            ?: throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")

    return namedGroups[name]
}
