/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import android.content.Context
import android.util.Log

class AndroidMain(val context: Context) {
    fun useContext() {
        context.getSystemService(Context.LOCATION_SERVICE)
    }

    fun useLog() {
        Log.d("test", CommonMain.toString())
    }

    companion object {
        fun useCommonMain() {
            println("useCommonMain: ${CommonMain}")
        }
    }
}
