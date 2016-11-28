package jquery.ui


//jquery UI
import jquery.JQuery

public inline fun JQuery.buttonset(): JQuery = asDynamic().buttonset()

public inline fun JQuery.dialog(): JQuery = asDynamic().dialog()

public inline fun JQuery.dialog(params: Json): JQuery = asDynamic().dialog(params)

public inline fun JQuery.dialog(mode: String, param: String): Any? = asDynamic().dialog(mode, param)

public inline fun JQuery.dialog(mode: String): JQuery = asDynamic().dialog(mode)

public inline fun JQuery.dialog(mode: String, param: String, value: Any?): JQuery = asDynamic().dialog(mode, param, value)

public inline fun JQuery.button(): JQuery = asDynamic().button()

public inline fun JQuery.accordion(): JQuery = asDynamic().accordion()

public inline fun JQuery.draggable(params: Json): JQuery = asDynamic().draggable(params)

public inline fun JQuery.selectable(): JQuery = asDynamic().selectable()
