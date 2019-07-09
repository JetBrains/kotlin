package com.example

import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.html
import kotlinx.html.stream.appendHTML

fun hello() = "hello"

fun main() {
    println(
            StringBuilder().appendHTML().html {
                body {
                    div {
                        a("https://kotlinlang.org") {
                            hello()
                        }
                    }
                }
            }
    )
}