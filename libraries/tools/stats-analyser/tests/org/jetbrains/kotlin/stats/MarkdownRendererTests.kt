/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownRendererTests {
    @Test
    fun testModulesModeRendering() {
        assertEquals(
            """# Stats for 2025-06-22T20:30:56

* Platform: JVM
* Has errors: true
* Modules count: 3
* Files count: 6
* Lines count: 768

# Total time

| Phase                         |          Absolute |              User |               Cpu |
| ----------------------------- | ----------------: | ----------------: | ----------------: |
| INIT                          |   10.00% (600 ms) |   10.00% (450 ms) |   10.00% (750 ms) |
| ANALYZE                       |  40.00% (2400 ms) |  40.00% (1800 ms) |  40.00% (3000 ms) |
| TRANSLATION to IR             |  20.00% (1200 ms) |   20.00% (900 ms) |  20.00% (1500 ms) |
| IR PRE-LOWERING               |     1.00% (60 ms) |     1.00% (45 ms) |     1.00% (75 ms) |
| ↳ IrPreLoweringDynamicStat1   |     0.50% (30 ms) |     0.50% (22 ms) |     0.50% (37 ms) |
| ↳ IrPreLoweringDynamicStat2   |     0.25% (15 ms) |     0.25% (11 ms) |     0.25% (18 ms) |
| ↳ IrPreLoweringDynamicStat3   |     0.25% (15 ms) |     0.25% (11 ms) |     0.25% (18 ms) |
| IR SERIALIZATION              |     1.00% (60 ms) |     1.00% (45 ms) |     1.00% (75 ms) |
| KLIB WRITING                  |     1.00% (60 ms) |     1.00% (45 ms) |     1.00% (75 ms) |
| IR LOWERING                   |     1.00% (60 ms) |     1.00% (45 ms) |     1.00% (75 ms) |
| BACKEND                       |   16.00% (960 ms) |   16.00% (720 ms) |  16.00% (1200 ms) |
|                               |                   |                   |                   |
| Find Java class               |    4.00% (240 ms) |    4.00% (180 ms) |    4.00% (300 ms) |
| Binary class from Kotlin file |    6.00% (360 ms) |    6.00% (270 ms) |    6.00% (450 ms) |
|                               |                   |                   |                   |
| TOTAL                         | 100.00% (6000 ms) | 100.00% (4500 ms) | 100.00% (7500 ms) |

# System stats

* JIT time: 6 ms
* GC stats:
  * gc-1: 100 ms (1 collections)
  * gc-2: 200 ms (2 collections)
  * gc-3: 300 ms (3 collections)

# Slowest modules

## By total time

| Module   |            Value |
| -------- | ---------------: |
| module-3 | 50.00% (3000 ms) |
| module-2 | 33.33% (2000 ms) |
| module-1 | 16.67% (1000 ms) |

## By analysis time

| Module   |            Value |
| -------- | ---------------: |
| module-3 | 50.00% (1200 ms) |
| module-2 |  33.33% (800 ms) |
| module-1 |  16.67% (400 ms) |

## By LPS (lines per second)

| Module   |      Value |
| -------- | ---------: |
| module-3 | 106.67 LPS |
| module-2 | 128.00 LPS |
| module-1 | 192.00 LPS |

""",
            MarkdownReportRenderer(StatsCalculator(TestData.moduleStats)).render()
        )
    }

    @Test
    fun testTimeStampModeRendering() {
        assertEquals(
            """# Stats for Aggregate

* Platform: JVM
* Has errors: false
* Modules count: 3
* Files count: 6
* Lines count: 768

# Average time

| Phase                         |          Absolute |              User |               Cpu |
| ----------------------------- | ----------------: | ----------------: | ----------------: |
| INIT                          |   10.00% (200 ms) |   10.00% (150 ms) |   10.00% (250 ms) |
| ANALYZE                       |   40.00% (800 ms) |   40.00% (600 ms) |  40.00% (1000 ms) |
| TRANSLATION to IR             |   20.00% (400 ms) |   20.00% (300 ms) |   20.00% (500 ms) |
| IR PRE-LOWERING               |     1.00% (20 ms) |     1.00% (15 ms) |     1.00% (25 ms) |
| ↳ IrPreLoweringDynamicStat1   |     0.50% (10 ms) |      0.50% (7 ms) |     0.50% (12 ms) |
| ↳ IrPreLoweringDynamicStat2   |      0.25% (5 ms) |      0.25% (3 ms) |      0.25% (6 ms) |
| ↳ IrPreLoweringDynamicStat3   |      0.25% (5 ms) |      0.25% (3 ms) |      0.25% (6 ms) |
| IR SERIALIZATION              |     1.00% (20 ms) |     1.00% (15 ms) |     1.00% (25 ms) |
| KLIB WRITING                  |     1.00% (20 ms) |     1.00% (15 ms) |     1.00% (25 ms) |
| IR LOWERING                   |     1.00% (20 ms) |     1.00% (15 ms) |     1.00% (25 ms) |
| BACKEND                       |   16.00% (320 ms) |   16.00% (240 ms) |   16.00% (400 ms) |
|                               |                   |                   |                   |
| Find Java class               |     4.00% (80 ms) |     4.00% (60 ms) |    4.00% (100 ms) |
| Binary class from Kotlin file |    6.00% (120 ms) |     6.00% (90 ms) |    6.00% (150 ms) |
|                               |                   |                   |                   |
| TOTAL                         | 100.00% (2000 ms) | 100.00% (1500 ms) | 100.00% (2500 ms) |

# System stats (Average)

* JIT time: 2 ms
* GC stats:
  * gc-1: 100 ms (1 collections)
  * gc-2: 200 ms (2 collections)
  * gc-3: 300 ms (3 collections)

# Slowest runs

## By total time

| Time Stamp          |            Value |
| ------------------- | ---------------: |
| 2025-06-22T20:30:56 | 50.00% (3000 ms) |
| 2025-06-21T20:30:56 | 33.33% (2000 ms) |
| 2025-06-20T20:30:56 | 16.67% (1000 ms) |

## By analysis time

| Time Stamp          |            Value |
| ------------------- | ---------------: |
| 2025-06-22T20:30:56 | 50.00% (1200 ms) |
| 2025-06-21T20:30:56 |  33.33% (800 ms) |
| 2025-06-20T20:30:56 |  16.67% (400 ms) |

## By LPS (lines per second)

| Time Stamp          |      Value |
| ------------------- | ---------: |
| 2025-06-22T20:30:56 | 106.67 LPS |
| 2025-06-21T20:30:56 | 128.00 LPS |
| 2025-06-20T20:30:56 | 192.00 LPS |

""",
            MarkdownReportRenderer(StatsCalculator(TestData.timeStampStats)).render()
        )
    }
}