/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#ifdef __OBJC__
#define OBJC_FORWARD_DECLARE(clazz) @class clazz
#else
#define OBJC_FORWARD_DECLARE(clazz) class clazz
#endif
