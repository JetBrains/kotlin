/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

kotlin {
    linuxX64("linuxA") {
        attributes {
            attribute(Attribute.of("variant", String::class.java), "a")
        }
    }
    linuxX64("linuxB") {
        attributes {
            attribute(Attribute.of("variant", String::class.java), "b")
        }
    }
}
