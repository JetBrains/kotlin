// K2_ONLY
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.validate
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