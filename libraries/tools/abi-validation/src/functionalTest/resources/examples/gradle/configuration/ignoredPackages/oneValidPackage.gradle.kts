/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

configure<kotlinx.validation.ApiValidationExtension> {
    ignoredPackages.add("com.company")
}