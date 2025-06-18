// DUMP_IR

// FILE: main.kt
package com.example.home

import androidx.compose.runtime.Composable
import com.example.sam.TestInterfaceOne

@Composable
fun IssueA(arg: TestInterfaceOne?) {
}

// FILE: TestInterfaces.kt
package com.example.sam

interface TestInterfaceOne : TestInterfaceTwo

interface TestInterfaceTwo {
    fun x(): Int
}