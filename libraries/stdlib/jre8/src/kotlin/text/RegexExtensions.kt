/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("RegexExtensionsJRE8Kt")
package kotlin.text

/**
 * Returns a named group with the specified [name].
 *
 * @return An instance of [MatchGroup] if the group with the specified [name] was matched or `null` otherwise.
 * @throws [UnsupportedOperationException] if getting named groups isn't supported on the current platform.
 */
@SinceKotlin("1.1")
public operator fun MatchGroupCollection.get(name: String): MatchGroup? {
    val namedGroups = this as? MatchNamedGroupCollection ?:
            throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")

    return namedGroups[name]
}
