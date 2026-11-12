// FILE: ok-downloader.kt
package test

fun download(url: String): Int = url.length

val retries: Int = 3

// FILE: App.android.kt
package test

fun platform(): String = "android"

// FILE: generated-names.kt
@file:JvmName("GeneratedNames")
package test

fun name(): String = "a"
