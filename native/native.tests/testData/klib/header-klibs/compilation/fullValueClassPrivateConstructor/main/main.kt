package app

import lib.*

fun sum(secret: Secret) = secret.a + secret.b

fun runAppAndReturnOk(): String = if (sum(twice(Secret.of(1))) == 4) "OK" else "Fail"
