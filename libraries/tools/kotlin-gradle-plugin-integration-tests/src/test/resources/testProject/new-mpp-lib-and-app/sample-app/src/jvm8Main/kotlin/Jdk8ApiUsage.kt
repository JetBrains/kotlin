package com.example.app

import kotlin.streams.asStream

fun useJdk8Api() = listOf(1, 2, 3).asSequence().asStream()