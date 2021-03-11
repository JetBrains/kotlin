// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.AddActivityToManifest
// CHECK_MANIFEST
package com.myapp

import android.app.Activity

class Test {
    class <caret>MyActivity : Activity()
}