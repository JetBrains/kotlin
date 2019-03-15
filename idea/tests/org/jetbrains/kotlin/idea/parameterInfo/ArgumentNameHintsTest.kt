/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class ArgumentNameHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun check(text: String) {
        myFixture.configureByText("A.kt", text)
        myFixture.testInlays()
    }

    fun `test insert literal arguments`() {
        check("""
 fun test(file: File) {
  val testNow = System.currentTimeMillis() > 34000
  val times = 1
  val pi = 4.0f
  val title = "Testing..."
  val ch = 'q';

  configure(<hint text="testNow:" />true, <hint text="times:" />555, <hint text="pii:" />3.141f, <hint text="title:" />"Huge Title", <hint text="terminate:" />'c', <hint text="file:" />null)
  configure(testNow, shouldIgnoreRoots(), fourteen, pi, title, c, file)
 }

 fun configure(testNow: Boolean, times: Int, pii: Float, title: String, terminate: Char, file: File) {
  System.out.println()
  System.out.println()
 }""")
    }

    fun `test do not show for Exceptions`() {
        check("""
  fun test() {
    val t = IllegalStateException("crime");
  }""")
    }

    fun `test single varargs hint`() {
        check("""
  fun main() {
    testBooleanVarargs(<hint text="test:" />13, <hint text="...booleans:" />false)
  }

  fun testBooleanVarargs(test: Int, vararg booleans: Boolean): Boolean {
    return false
  }
}
""")
    }

    fun `test no hint if varargs null`() {
        check("""
  fun main() {
    testBooleanVarargs(<hint text="test:" />13)
  }

  fun testBooleanVarargs(test: Int, vararg booleans: Boolean): Boolean {
    return false
  }
""")
    }


    fun `test multiple vararg hint`() {
        check("""
  fun main() {
    testBooleanVarargs(<hint text="test:" />13, <hint text="...booleans:" />false, true, false)
  }

  fun testBooleanVarargs(test: Int, vararg booleans: Boolean): Boolean {
    return false
  }
""")
    }

    fun `test inline positive and negative numbers`() {
        check("""
  fun main() {
    val obj = Any()
    count(<hint text="test:"/>-1, obj);
    count(<hint text="test:"/>+1, obj);
  }

  fun count(test: Int, obj: Any) {
  }
}
""")
    }

    fun `test show ambiguous`() {
        check("""
  fun main() {
    test(<hint text="a:"/>10, x);
  }
  fun test(a: Int, bS: String) {}
  fun test(a: Int, bI: Int) {}
""")
    }

    fun `test show non-ambiguous overload`() {
        check("""
  fun main() {
    test(<hint text="a:"/>10, <hint text="bI:"/>15);
  }
  fun test() {}
  fun test(a: Int, bS: String) {}
  fun test(a: Int, bI: Int) {}
""")
    }

    fun `test show ambiguous constructor`() {
        check("""
  fun main() {
    X(<hint text="a:"/>10, x);
  }
}

class X {
  constructor(a: Int, bI: Int) {}
  constructor(a: Int, bS: String) {}
}
""")
    }

    fun `test invoke`() {
        check("""
  fun main() {
    val x = X()
    x(<hint text="a:"/>10, x);
  }
}

class X {
  operator fun invoke(a: Int, bI: Int) {}
}
""")
    }

    fun `test annotation`() {
        check("""
annotation class ManyArgs(val name: String, val surname: String)
@ManyArgs(<hint text="name:"/>"Ilya", <hint text="surname:"/>"Sergey") class AnnotatedMuch
""")
    }

    fun `test functional type`() {
        check("""
            fun <T> T.test(block: (T) -> Unit) = block(this)
        """)
    }

    fun `test functional type with parameter name`() {
        check("""
            fun <T> T.test(block: (receiver: T, Int) -> Unit) = block(<hint text="receiver:"/>this, 0)
        """)
    }

    fun `test dynamic`() {
        check("""fun foo(x: dynamic) {
            x.foo("123")
        }""")
    }

    fun `test spread`() {
        check("""fun foo(vararg x: Int) {
            intArrayOf(1, 0).apply { foo(<hint text="...x:" />*this) }
        }""")
    }

    fun `test line break`() {
        check("""fun foo(vararg x: Int) {
            foo(<hint text="...x:" />
                1,
                2,
                3
            ) }
        }"""
        )
    }

    fun `test incomplete Pair call`() {
        check("val x = Pair(1, )")
    }
}
