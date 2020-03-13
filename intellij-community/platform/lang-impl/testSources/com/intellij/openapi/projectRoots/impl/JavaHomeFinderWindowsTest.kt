// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("SpellCheckingInspection")

package com.intellij.openapi.projectRoots.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JavaHomeFinderWindowsTest {

  @Test
  fun `parse basic Reg output`() {
    val text = """|
                  |HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\JDK\9.0.4
                  |    JavaHome    REG_SZ    C:\Java\1.9
                  |
                  |HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\JDK\11.0.6
                  |    JavaHome    REG_SZ    D:\Java\11.0.6
                  |
                  |HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\JDK\13.0.2
                  |    JavaHome    REG_SZ    F:\AnotherFolder\OracleJava\13.0.2
                  |
                  |End of search: 3 match(es) found.
                  |
    """.trimMargin()
    val paths = JavaHomeFinderWindows.gatherHomePaths(text)

    assertThat(paths).containsExactly("""C:\Java\1.9""", """D:\Java\11.0.6""", """F:\AnotherFolder\OracleJava\13.0.2""")
  }

  
}