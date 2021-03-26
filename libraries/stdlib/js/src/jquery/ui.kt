/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file: Suppress("DEPRECATION_ERROR")
package jquery.ui


//jquery UI
import jquery.JQuery
import kotlin.js.Json

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.buttonset(): JQuery = asDynamic().buttonset()

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.dialog(): JQuery = asDynamic().dialog()

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.dialog(params: Json): JQuery = asDynamic().dialog(params)

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.dialog(mode: String, param: String): Any? = asDynamic().dialog(mode, param)

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.dialog(mode: String): JQuery = asDynamic().dialog(mode)

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.dialog(mode: String, param: String, value: Any?): JQuery = asDynamic().dialog(mode, param, value)

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.button(): JQuery = asDynamic().button()

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.accordion(): JQuery = asDynamic().accordion()

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.draggable(params: Json): JQuery = asDynamic().draggable(params)

@Deprecated("Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.", level = DeprecationLevel.ERROR)
public inline fun JQuery.selectable(): JQuery = asDynamic().selectable()
