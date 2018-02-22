/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import konan.internal.ExportForCppRuntime

// This function is produced by the code generator given
// the '-entry foo.bar.main' flag. 
// It calls the requested entry point.
// The default is main(Array<String>):Unit in the root package.
@ExportForCppRuntime
external fun EntryPointSelector(args: Array<String>)

@ExportForCppRuntime
private fun Konan_start(args: Array<String>): Int {
    try {
        EntryPointSelector(args)

        // Successfully finished:
        return 0

    } catch (e: Throwable) {
        // TODO: may be add some more info.
        print("Uncaught exception from Kotlin's main: ")
        e.printStackTrace()
        return 1
    }
}
