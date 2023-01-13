/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package recursiveGenericArguments

class RecList<T: List<T>>(val value: T)

class RecFunc<T : () -> T>(val value: T)