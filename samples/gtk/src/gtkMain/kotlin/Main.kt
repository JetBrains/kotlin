/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gtk

import kotlinx.cinterop.*
import gtk3.*

// Note that all callback parameters must be primitive types or nullable C pointers.
fun <F : CFunction<*>> g_signal_connect(obj: CPointer<*>, actionName: String,
        action: CPointer<F>, data: gpointer? = null, connect_flags: GConnectFlags = 0u) {
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
                args.size, args.map { it.cstr.ptr }.toCValues())
    }
    g_object_unref(app)
    return status
}

fun main(args: Array<String>) {
    gtkMain(args)
}
