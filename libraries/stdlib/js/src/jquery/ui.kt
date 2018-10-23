@file: Suppress("DEPRECATION")
package jquery.ui


//jquery UI
import jquery.JQuery
import kotlin.js.Json

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.buttonset(): JQuery = asDynamic().buttonset()

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.dialog(): JQuery = asDynamic().dialog()

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.dialog(params: Json): JQuery = asDynamic().dialog(params)

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.dialog(mode: String, param: String): Any? = asDynamic().dialog(mode, param)

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.dialog(mode: String): JQuery = asDynamic().dialog(mode)

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.dialog(mode: String, param: String, value: Any?): JQuery = asDynamic().dialog(mode, param, value)

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.button(): JQuery = asDynamic().button()

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.accordion(): JQuery = asDynamic().accordion()

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.draggable(params: Json): JQuery = asDynamic().draggable(params)

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public inline fun JQuery.selectable(): JQuery = asDynamic().selectable()
