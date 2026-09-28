/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package linking.source

/**
 * Reference link [KotlinEnum] should resolve <p>
 * stuff stuff [KotlinEnum.ON_CREATE] should resolve <p>
 * stuff stuff [JavaEnum.ON_DECEIT] should resolve
 */
class KotlinLinker {}
