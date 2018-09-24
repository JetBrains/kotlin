// CORRECT_ERROR_TYPES

// FILE: a.kt
package test

import java.io.File
import java.io.File as JavaFile
import java.io.IOException
import java.io.IOException as JavaIOException

class TestA

// FILE: lib/File.java
package lib;
public class File {}

// FILE: lib/IOException.java
package lib;
public class IOException {}

// FILE: b.kt
package test

import java.io.File
import lib.File as LibFile
import java.io.IOException
import lib.IOException as LibIOException

interface TestB {
    fun a(): File
    fun b(): LibFile
    fun c(): IOException
    fun d(): LibIOException
}

// FILE: c.kt
@file:Suppress("UNRESOLVED_REFERENCE")
package test

import java.io.File as JavaFile
import lib.File as LibFile
import java.io.IOException as JavaIOException
import lib.IOException as LibIOException
import lib.FooBar as LibFooBar

interface TestC {
    fun a(): JavaFile
    fun b(): LibFile
    fun c(): JavaIOException
    fun d(): LibIOException

    fun e(): LibFooBar
}

// EXPECTED_ERROR(kotlin:17:5) cannot find symbol