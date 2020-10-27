/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#import "Types.h"

extern "C" id Kotlin_ObjCExport_ExceptionAsNSError(KRef exception, const TypeInfo** types);
extern "C" OBJ_GETTER(Kotlin_ObjCExport_NSErrorAsException, id error);
