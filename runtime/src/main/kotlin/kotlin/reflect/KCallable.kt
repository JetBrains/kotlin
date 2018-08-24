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

package kotlin.reflect

import kotlin.native.internal.FixmeReflection

/**
 * Represents a callable entity, such as a function or a property.
 *
 * @param R return type of the callable.
 */
@FixmeReflection
public interface KCallable<out R> : KAnnotatedElement {
    /**
     * The name of this callable as it was declared in the source code.
     * If the callable has no name, a special invented name is created.
     * Nameless callables include:
     * - constructors have the name "<init>",
     * - property accessors: the getter for a property named "foo" will have the name "<get-foo>",
     *   the setter, similarly, will have the name "<set-foo>".
     */
    public val name: String

    /**
     * The type of values returned by this callable.
     */
    public val returnType: KType
}
