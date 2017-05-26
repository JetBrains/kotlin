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

import kotlinx.cinterop.*
import gtk3.*

// Note that all callback parameters must be primitive types or nullable C pointers.
fun <F : CFunction<*>> g_signal_connect(obj: CPointer<*>, actionName: String,
        action: CPointer<F>, data: gpointer? = null, connect_flags: Int = 0) {
    g_signal_connect_data(obj.reinterpret(), actionName, action.reinterpret(),
            data = data, destroy_data = null, connect_flags = connect_flags)

}

fun activate(app: CPointer<GtkApplication>?, user_data: gpointer?) {
    val windowWidget = gtk_application_window_new(app)!!
    val window = windowWidget.reinterpret<GtkWindow>()
    gtk_window_set_title(window, "Window")
    gtk_window_set_default_size(window, 200, 200)

    val button_box = gtk_button_box_new(
            GtkOrientation.GTK_ORIENTATION_HORIZONTAL)!!
    gtk_container_add(window.reinterpret(), button_box)

    val button = gtk_button_new_with_label("Konan говорит: click me!")!!
    g_signal_connect(button, "clicked",
            staticCFunction { _: CPointer<GtkWidget>?, _: gpointer? -> println("Hi Kotlin")
            })
    g_signal_connect(button, "clicked",
            staticCFunction { widget: CPointer<GtkWidget>? ->
                gtk_widget_destroy(widget)
            },
            window, G_CONNECT_SWAPPED)
    gtk_container_add (button_box.reinterpret(), button)

    gtk_widget_show_all(windowWidget)
}

fun gtkMain(args: Array<String>): Int {
    val app = gtk_application_new("org.gtk.example", G_APPLICATION_FLAGS_NONE)!!
    g_signal_connect(app, "activate", staticCFunction(::activate))
    val status = memScoped {
        g_application_run(app.reinterpret(),
                args.size, args.map { it.cstr.getPointer(memScope) }.toCValues())
    }
    g_object_unref(app)
    return status
}

fun main(args: Array<String>) {
    gtkMain(args)
}
