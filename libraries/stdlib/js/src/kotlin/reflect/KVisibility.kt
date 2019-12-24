/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin.reflect

/**
 * Visibility is an aspect of a Kotlin declaration regulating where that declaration is accessible in the source code.
 * Visibility can be changed with one of the following modifiers: `public`, `protected`, `internal`, `private`.
 *
 * Note that some Java visibilities such as package-private and protected (which also gives access to items from the same package)
 * cannot be represented in Kotlin, so there's no [KVisibility] value corresponding to them.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/visibility-modifiers.html)
 * for more information.
 */
@SinceKotlin("1.1")
enum class KVisibility {
    /**
     * Visibility of declarations marked with the `public` modifier, or with no modifier at all.
     */
    PUBLIC,

    /**
     * Visibility of declarations marked with the `protected` modifier.
     */
    PROTECTED,

    /**
     * Visibility of declarations marked with the `internal` modifier.
     */
    INTERNAL,

    /**
     * Visibility of declarations marked with the `private` modifier.
     */
    PRIVATE,
}
