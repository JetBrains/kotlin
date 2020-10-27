/*
 * Copyright 2010-2020 JetBrains s.r.o.
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

#import "Memory.h"
#import "ObjCExportExceptionDetails.h"
#import "ObjCExport.h"

#if KONAN_OBJC_INTEROP

#import <Foundation/NSException.h>

//! TODO: Use not_null signature.
OBJ_GETTER(Kotlin_ObjCExport_ExceptionDetails, KRef /*thiz*/, KRef exceptionHolder) {
  if (NSException* exception = (NSException*)Kotlin_ObjCExport_refToObjC(exceptionHolder)) {
    RuntimeAssert([exception isKindOfClass:[NSException class]], "Illegal type: NSException expected");
    NSString* ret = [NSString stringWithFormat: @"%@:: %@", exception.name, exception.reason];
    RETURN_RESULT_OF(Kotlin_Interop_CreateKStringFromNSString, ret);
  }

  RETURN_OBJ(nullptr);
}

#else // KONAN_OBJC_INTEROP

OBJ_GETTER(Kotlin_ObjCExport_ExceptionDetails, KRef /*thiz*/, KRef /*exceptionHolder*/) {
  RETURN_OBJ(nullptr);
}

#endif
