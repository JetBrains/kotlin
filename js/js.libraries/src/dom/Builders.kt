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

package kotlin.dom

import org.w3c.dom.*

/**
 * Creates a new element with the specified [name].
 *
 * The element is initialized with the speicifed [init] function.
 */
public fun Document.createElement(name: String, init: Element.() -> Unit): Element = createElement(name).apply(init)

/**
 * Appends a newly created element with the specified [name] to this element.
 *
 * The element is initialized with the speicifed [init] function.
 */
public fun Element.appendElement(name: String, init: Element.() -> Unit): Element =
        ownerDocument!!.createElement(name, init).also { appendChild(it) }

