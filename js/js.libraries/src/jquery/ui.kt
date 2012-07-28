package js.jquery.ui


//jquery UI
import js.jquery.JQuery
import js.native
import js.Json

native
fun JQuery.buttonset()  : JQuery = js.noImpl;
native
fun JQuery.dialog() : JQuery = js.noImpl;
native
fun JQuery.dialog(params : Json) : JQuery = js.noImpl
native
fun JQuery.dialog(mode : String, param : String) : Any? = js.noImpl
native
fun JQuery.dialog(mode : String) : JQuery = js.noImpl
native
fun JQuery.dialog(mode : String, param : String, value : Any?) : JQuery = js.noImpl
native
fun JQuery.button() : JQuery = js.noImpl;
native
fun JQuery.accordion() : JQuery = js.noImpl
native
fun JQuery.draggable(params : Json) : JQuery = js.noImpl
native
fun JQuery.selectable() : JQuery = js.noImpl
