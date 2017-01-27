package jquery.ui


//jquery UI
import jquery.JQuery
import kotlin.js.Json

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.buttonset(): JQuery = asDynamic().buttonset()

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.dialog(): JQuery = asDynamic().dialog()

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.dialog(params: Json): JQuery = asDynamic().dialog(params)

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.dialog(mode: String, param: String): Any? = asDynamic().dialog(mode, param)

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.dialog(mode: String): JQuery = asDynamic().dialog(mode)

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.dialog(mode: String, param: String, value: Any?): JQuery = asDynamic().dialog(mode, param, value)

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.button(): JQuery = asDynamic().button()

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.accordion(): JQuery = asDynamic().accordion()

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.draggable(params: Json): JQuery = asDynamic().draggable(params)

@Deprecated("JQuery is going to be removed from the standard library")
public inline fun JQuery.selectable(): JQuery = asDynamic().selectable()
