package com.example

import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.p
import kotlin.browser.document

fun hello() = "hello"

fun main() {
    document.create.div {
        p { hello() }
    }
    console.log(hello())
}