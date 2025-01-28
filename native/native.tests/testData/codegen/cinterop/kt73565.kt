// TARGET_BACKEND: NATIVE
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.test.*
import kotlinx.cinterop.*

fun box(): String {
    // memcmp
    val data1 = floatArrayOf(1F, 2F, 3F, 4F, 5F, 6F)
    val data2 = floatArrayOf(1F, 2F, 3F, 4F, 5F, 6F)
    val data3 = floatArrayOf(1F, 2F, 3F, 4F, 4F, 6F)

    data1.usePinned { pinnedMemA ->
        data2.usePinned { pinnedMemB ->
            if (pinnedMemA.addressOf(0).compareBlock(pinnedMemB.addressOf(0), data1.size) != 0) {
                return "memcmp test failed for equal data"
            }
        }
        data3.usePinned { pinnedMemB ->
            if (pinnedMemA.addressOf(0).compareBlock(pinnedMemB.addressOf(0), data1.size) == 0) {
                return "memcmp test failed for unequal data"
            }
        }
    }

    // memset
    data1.usePinned { pinned ->
        pinned.addressOf(0).setBlock(0, data1.size)
    }
    if (data1.contentEquals(data2)) {
        return "memset test failed"
    }

    // memcpy
    data1.usePinned { pinnedDest ->
        data2.usePinned { pinnedSrc ->
            pinnedSrc.addressOf(0).copyTo(pinnedDest.addressOf(0), data1.size)
        }
    }
    if (!data1.contentEquals(data2)) {
        return "memcpy test failed"
    }

    // memmove
    data1.usePinned { pinned ->
        pinned.addressOf(0).moveTo(pinned.addressOf(1), 2)
    }
    if (!data1.contentEquals(floatArrayOf(1F, 1F, 2F, 4F, 5F, 6F))) {
        return "memmove test failed with overlapping data"
    }

    return "OK"
}