/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.io.encoding

import samples.*
import java.io.*
import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
class Base64StreamsSample {
    @Sample
    fun base64InputStream() {
        ByteArrayInputStream("SGVsbG8gV29ybGQh".toByteArray()).decodingWith(Base64.Default).use {
            assertPrints(it.readBytes().decodeToString(), "Hello World!")
        }

        // Padding could be omitted
        ByteArrayInputStream("UGFkOg".toByteArray()).decodingWith(Base64.Default).use {
            assertPrints(it.readBytes().decodeToString(), "Pad:")
        }

        ByteArrayInputStream("UGFkOg==... and everything else".toByteArray()).use { input ->
            input.decodingWith(Base64.Default).also {
                // Reads only Base64-encoded part, including padding
                assertPrints(it.readBytes().decodeToString(), "Pad:")
            }
            // The original stream will only contain remaining bytes
            assertPrints(input.readBytes().decodeToString(), "... and everything else")
        }
    }

    @Sample
    fun base64OutputStream() {
        ByteArrayOutputStream().also { out ->
            out.encodingWith(Base64.Default).use {
                it.write("Hello World!!".encodeToByteArray())
            }
            assertPrints(out.toString(), "SGVsbG8gV29ybGQhIQ==")
        }
    }
}
