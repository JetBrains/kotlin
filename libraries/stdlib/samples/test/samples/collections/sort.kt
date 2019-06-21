/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.collections

import samples.*
import java.time.LocalDateTime

@RunWith(Enclosed::class)
class Sort {

    class Usage {

        @Sample
        fun sortArray() {
            val intArray = intArrayOf(4, 3, 2, 1)

            assertPrints(
                intArray.joinToString(),
                "4, 3, 2, 1"
            )

            intArray.sort()

            assertPrints(
                intArray.joinToString(),
                "1, 2, 3, 4"
            )
        }

        @Sample
        fun sortMutableList() {
            val mutableList = mutableListOf(4, 3, 2, 1)

            assertPrints(
                mutableList.joinToString(),
                "4, 3, 2, 1"
            )

            mutableList.sort()

            assertPrints(
                mutableList.joinToString(),
                "1, 2, 3, 4"
            )
        }

        @Sample
        fun sortComparable() {
            class Envelope(val sent: LocalDateTime) : Comparable<Envelope> {
                override fun compareTo(other: Envelope): Int {
                    return sent.compareTo(other.sent)
                }

                override fun toString(): String {
                    return "Envelope(sent=$sent)"
                }


            }

            val now = LocalDateTime.of(2019, 6, 21, 10, 39)

            val mail = arrayOf(
                Envelope(now.plusMinutes(2)),
                Envelope(now.plusMinutes(1)),
                Envelope(now)
            )

            assertPrints(
                mail.joinToString(),
                "Envelope(sent=2019-06-21T10:41), Envelope(sent=2019-06-21T10:40), Envelope(sent=2019-06-21T10:39)"
            )

            mail.sort()

            assertPrints(
                mail.joinToString(),
                "Envelope(sent=2019-06-21T10:39), Envelope(sent=2019-06-21T10:40), Envelope(sent=2019-06-21T10:41)"
            )
        }

        @Sample
        fun sortRangeOfComparable() {
            class Envelope(val sent: LocalDateTime) : Comparable<Envelope> {
                override fun compareTo(other: Envelope): Int {
                    return sent.compareTo(other.sent)
                }

                override fun toString(): String {
                    return "Envelope(sent=$sent)"
                }

            }

            val now = LocalDateTime.of(2019, 6, 21, 10, 39)

            val mail = arrayOf(
                Envelope(now.plusMinutes(2)),
                Envelope(now.plusMinutes(1)),
                Envelope(now)
            )

            assertPrints(
                mail.joinToString(),
                "Envelope(sent=2019-06-21T10:41), Envelope(sent=2019-06-21T10:40), Envelope(sent=2019-06-21T10:39)"
            )

            mail.sort(0, 2)

            assertPrints(
                mail.joinToString(),
                "Envelope(sent=2019-06-21T10:40), Envelope(sent=2019-06-21T10:41), Envelope(sent=2019-06-21T10:39)"
            )
        }

        @Sample
        fun sortRangeOfArray() {
            val intArray = intArrayOf(4, 3, 2, 1)

            assertPrints(
                intArray.joinToString(),
                "4, 3, 2, 1"
            )

            intArray.sort(0, 3)

            assertPrints(
                intArray.joinToString(),
                "2, 3, 4, 1"
            )
        }

    }

}
