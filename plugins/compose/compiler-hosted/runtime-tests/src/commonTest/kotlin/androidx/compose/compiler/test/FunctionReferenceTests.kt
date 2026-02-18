// K2_ONLY
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.test

import androidx.compose.runtime.*
import androidx.compose.runtime.mock.*
import kotlin.test.Test

// This class is on top of the file to avoid handling order when loading generated classes
private interface FunctionRefInterface {
    @Composable
    fun TextDefault(int: Int = 0) {
        Text("TextDefault $int")
    }
}

class FunctionReferenceTests {
    @Test
    fun functionReference() = compositionTest {
        compose {
            Linear(::SimpleText)
        }

        validate {
            Linear {
                Text("SimpleText")
            }
        }
    }

    @Test
    fun adoptedReference() = compositionTest {
        compose {
            Linear(::TextDefault)
        }

        validate {
            Linear {
                Text("TextDefault 0")
            }
        }
    }

    @Test
    fun referenceReturnType() = compositionTest {
        var check by mutableStateOf(true)
        compose {
            val number: @Composable (Int) -> Int = if (check) ::Number else ::OtherNumber
            Linear {
                Text(0, number)
            }
        }

        validate {
            Linear {
                Text("Text ${if (check) 0 else 1}")
            }
        }

        check = false
        advance()
        revalidate()
    }

    @Test
    fun default() = compositionTest {
        compose {
            LinearCount(3, ::TextDefault)
        }

        validate {
            Linear {
                Text("TextDefault 0")
                Text("TextDefault 1")
                Text("TextDefault 2")
            }
        }
    }

    @Test
    fun dispatchReceiver() = compositionTest {
        val sample = FunctionRefClass()
        compose {
            Linear(sample::TextDefault)
            LinearCount(3, sample::TextDefault)
        }

        validate {
            Linear {
                Text("TextDefault 0")
            }
            Linear {
                Text("TextDefault 0")
                Text("TextDefault 1")
                Text("TextDefault 2")
            }
        }
    }

    @Test
    fun extensionReceiver() = compositionTest {
        val sample = FunctionRefClass()
        compose {
            Linear(sample::TextDefaultExt)
            LinearCount(3, sample::TextDefaultExt)
        }

        validate {
            Linear {
                Text("TextDefault 0")
            }
            Linear {
                Text("TextDefault 0")
                Text("TextDefault 1")
                Text("TextDefault 2")
            }
        }
    }

    @Test
    fun vararg() = compositionTest {
        compose {
            Linear(::TextVararg)
            LinearCount(1, ::TextVararg)
            LinearMultiple(::TextVararg)
        }

        validate {
            Linear {
                Text("TextDefault 0")
            }
            Linear {
                Text("TextDefault 0")
            }
            Linear {
                Text("TextDefault 0")
                Text("TextDefault 1")
            }
        }
    }

    @Test
    fun interfaceReceiver() = compositionTest {
        val sample = object : FunctionRefInterface {}
        val sample2 = object : FunctionRefInterface {
            @Composable
            override fun TextDefault(int: Int) {
                Text("TextDefault2 $int")
            }
        }
        var check by mutableStateOf(true)
        compose {
            val s = if (check) sample else sample2

            Linear(s::TextDefault)
            LinearCount(3, s::TextDefault)
        }

        validate {
            val text = if (check) "TextDefault" else "TextDefault2"
            Linear {
                Text("$text 0")
            }
            Linear {
                Text("$text 0")
                Text("$text 1")
                Text("$text 2")
            }
        }

        check = false
        advance()
        revalidate()
    }

    @Test
    fun inline() = compositionTest {
        compose {
            InlineLinear(::InlineText)
            InlineLinear(::InlineTextDefault)
            LinearCount(3, ::InlineTextDefault)
        }

        validate {
            Linear {
                Text("InlineText")
            }
            Linear {
                Text("InlineTextDefault 0")
            }
            Linear {
                Text("InlineTextDefault 0")
                Text("InlineTextDefault 1")
                Text("InlineTextDefault 2")
            }
        }
    }

    @Test
    fun generics() = compositionTest {
        compose {
            GenericLinear(0, ::GenericText)
            GenericLinear(0, ::GenericTextWDefault)
        }

        validate {
            Linear {
                Text("GenericText 0")
            }
            Linear {
                Text("GenericText 0")
            }
        }
    }

    @Test
    fun lambda() = compositionTest {
        compose {
            val lambda = @Composable { Text("test") }
            Linear(lambda::invoke)
        }

        validate {
            Linear {
                Text("test")
            }
        }
    }
}

@Composable
private fun SimpleText() {
    Text("SimpleText")
}

@Composable
private fun TextDefault(int: Int = 0) {
    Text("TextDefault $int")
}

@Composable
private inline fun InlineText() {
    Text("InlineText")
}

@Composable
private inline fun InlineTextDefault(int: Int = 0) {
    Text("InlineTextDefault $int")
}

@Composable
private fun LinearCount(count: Int, content: @Composable (Int) -> Unit) {
    Linear {
        repeat(count) {
            content(it)
        }
    }
}

@Composable
private fun LinearMultiple(content: @Composable (Int, Int) -> Unit) {
    Linear {
        content(0, 1)
    }
}

@Composable
private fun TextVararg(vararg int: Int = intArrayOf(0)) {
    int.forEach {
        TextDefault(it)
    }
}

@Composable
private fun Number(int: Int): Int {
    remember { "" } // this is to ensure remember collides without a group
    return int
}

@Composable
private fun OtherNumber(int: Int, def: Int = 1) =
    remember { int + def }

@Composable
private fun Text(n: Int, number: @Composable (Int) -> Int) {
    Text("Text ${number(n)}")
}

@Composable
private fun <T> GenericLinear(
    t: T,
    content: @Composable (T) -> Unit
) {
    Linear {
        content(t)
    }
}

@Composable
private fun <T> GenericText(
    t: T
) {
    Text("GenericText $t")
}

@Composable
private fun <T> GenericTextWDefault(
    t: T? = null
) {
    Text("GenericText $t")
}

private class FunctionRefClass {
    @Composable
    fun TextDefault(int: Int = 0) {
        Text("TextDefault $int")
    }
}

@Composable
private fun FunctionRefClass.TextDefaultExt(int: Int = 0) {
    Text("TextDefault $int")
}