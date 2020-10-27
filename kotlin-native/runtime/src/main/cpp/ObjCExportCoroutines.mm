/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_OBJC_INTEROP

#import <pthread.h>
#import <Foundation/NSException.h>
#import <Foundation/NSObject.h>

#import "ObjCExport.h"
#import "ObjCExportErrors.h"

typedef void (^Completion)(id _Nullable, NSError* _Nullable);

extern "C" void Kotlin_ObjCExport_runCompletionSuccess(KRef completionHolder, KRef result) {
  Completion completion = (Completion)GetAssociatedObject(completionHolder);
  completion(Kotlin_ObjCExport_refToObjC(result), nullptr);
}

extern "C" void Kotlin_ObjCExport_runCompletionFailure(
  KRef completionHolder,
  KRef exception,
  const TypeInfo** exceptionTypes
) {
  id error = Kotlin_ObjCExport_ExceptionAsNSError(exception, exceptionTypes);
  Completion completion = (Completion)GetAssociatedObject(completionHolder);
  completion(nullptr, error);
}

extern "C" OBJ_GETTER(Kotlin_ObjCExport_createContinuationArgumentImpl,
    KRef completionHolder, const TypeInfo** exceptionTypes);

extern "C" OBJ_GETTER(Kotlin_ObjCExport_createContinuationArgument, id completion, const TypeInfo** exceptionTypes) {
  if (pthread_main_np() != 1) {
    [NSException raise:NSGenericException
        format:@"Calling Kotlin suspend functions from Swift/Objective-C is currently supported only on main thread"];
  }
  ObjHolder slot;
  KRef completionHolder = AllocInstanceWithAssociatedObject(theForeignObjCObjectTypeInfo,
      objc_retainBlock(completion), slot.slot());

  RETURN_RESULT_OF(Kotlin_ObjCExport_createContinuationArgumentImpl, completionHolder, exceptionTypes);
}

extern "C" void Kotlin_ObjCExport_resumeContinuationSuccess(KRef continuation, KRef result);
extern "C" void Kotlin_ObjCExport_resumeContinuationFailure(KRef continuation, KRef exception);

extern "C" void Kotlin_ObjCExport_resumeContinuation(KRef continuation, id result, id error) {
  ObjHolder holder;

  if (error != nullptr) {
    if (result != nullptr) {
      [NSException raise:NSGenericException
          format:@"Kotlin completion handler is called with both result (%@) and error (%@) specified",
          result, error];
    }

    KRef exception = Kotlin_ObjCExport_NSErrorAsException(error, holder.slot());
    Kotlin_ObjCExport_resumeContinuationFailure(continuation, exception);
  } else {
    KRef kotlinResult = Kotlin_ObjCExport_refFromObjC(result, holder.slot());
    Kotlin_ObjCExport_resumeContinuationSuccess(continuation, kotlinResult);
  }
}

#endif
