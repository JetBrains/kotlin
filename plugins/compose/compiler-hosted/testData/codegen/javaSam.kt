// DUMP_IR

// FILE: main.kt
package com.example.home

import androidx.compose.runtime.Composable
import com.example.interfaces.I

@Composable
fun foo(arg: I?) {
}

// FILE: interface.java
package com.example.interfaces;
import androidx.compose.runtime.Composable;

@FunctionalInterface
public interface I {
    @Composable
    void foo();
}
