// "Import" "true"
// ERROR: Unresolved reference: R
// DO_NOT_IMPORT: android.support.v7.appcompat.R

package com.myapp.activity

import com.myapp.R

fun test() {
    val a = <caret>R.layout.activity_test_kotlin
}