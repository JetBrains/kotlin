/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

#if KONAN_OBJC_INTEROP

#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSString.h>

#import "Exceptions.h"
#import "ObjCExport.h"
#import "Porting.h"
#import "Runtime.h"
#import "Mutex.hpp"

#import "ObjCExportErrors.h"

extern "C" OBJ_GETTER(Kotlin_Throwable_getMessage, KRef throwable);
extern "C" OBJ_GETTER(Kotlin_ObjCExport_getWrappedError, KRef throwable);

static void printlnMessage(const char* message) {
  konan::consolePrintf("%s\n", message);
}

extern "C" RUNTIME_NORETURN void Kotlin_ObjCExport_trapOnUndeclaredException(KRef exception) {
  printlnMessage("Function doesn't have or inherit @Throws annotation and thus exception isn't propagated "
                 "from Kotlin to Objective-C/Swift as NSError.\n"
                 "It is considered unexpected and unhandled instead. Program will be terminated.");

  TerminateWithUnhandledException(exception);
}

static char kotlinExceptionOriginChar;

static bool isExceptionOfType(KRef exception, const TypeInfo** types) {
  if (types) for (int i = 0; types[i] != nullptr; ++i) {
    // TODO: use fast instance check when possible.
    if (IsInstance(exception, types[i])) return true;
  }

  return false;
}

extern "C" id Kotlin_ObjCExport_ExceptionAsNSError(KRef exception, const TypeInfo** types) {
  ObjHolder errorHolder;
  KRef error = Kotlin_ObjCExport_getWrappedError(exception, errorHolder.slot());
  if (error != nullptr) {
    // Thrown originally by Swift/Objective-C.
    // Not actually a Kotlin exception, so don't check if it matches [types].
    return Kotlin_ObjCExport_refToObjC(error);
  }

  if (!isExceptionOfType(exception, types)) {
    printlnMessage("Exception doesn't match @Throws-specified class list and thus isn't propagated "
                   "from Kotlin to Objective-C/Swift as NSError.\n"
                   "It is considered unexpected and unhandled instead. Program will be terminated.");
    TerminateWithUnhandledException(exception);
  }

  NSMutableDictionary<NSErrorUserInfoKey, id>* userInfo = [[NSMutableDictionary new] autorelease];
  userInfo[@"KotlinException"] = Kotlin_ObjCExport_refToObjC(exception);
  userInfo[@"KotlinExceptionOrigin"] = @(&kotlinExceptionOriginChar); // Support for different Kotlin runtimes loaded.

  ObjHolder messageHolder;
  KRef message = Kotlin_Throwable_getMessage(exception, messageHolder.slot());
  NSString* description = Kotlin_Interop_CreateNSStringFromKString(message);
  if (description != nullptr) {
    userInfo[NSLocalizedDescriptionKey] = description;
  }

  return [NSError errorWithDomain:@"KotlinException" code:0 userInfo:userInfo];
}

extern "C" void Kotlin_ObjCExport_RethrowExceptionAsNSError(KRef exception, id* outError, const TypeInfo** types) {
    id error = Kotlin_ObjCExport_ExceptionAsNSError(exception, types); // Also traps on unexpected exception.
    if (outError != nullptr) *outError = error;
}

extern "C" OBJ_GETTER(Kotlin_ObjCExport_NSErrorAsExceptionImpl, KRef message, KRef error);

extern "C" OBJ_GETTER(Kotlin_ObjCExport_NSErrorAsException, id error) {
  NSString* description;

  NSError* e = (NSError*) error;
  if (e != nullptr) {
    auto userInfo = e.userInfo;
    if (userInfo != nullptr) {
      id kotlinException = userInfo[@"KotlinException"];
      id kotlinExceptionOrigin = userInfo[@"KotlinExceptionOrigin"];
      if (kotlinException != nullptr &&
            kotlinExceptionOrigin != nullptr && [kotlinExceptionOrigin isEqual:@(&kotlinExceptionOriginChar)]
      ) {
        RETURN_RESULT_OF(Kotlin_ObjCExport_refFromObjC, kotlinException);
      }
    }
    description = e.localizedDescription;
  } else {
    description = nullptr;
  }

  ObjHolder messageHolder, errorHolder;
  KRef message = Kotlin_Interop_CreateKStringFromNSString(description, messageHolder.slot());
  KRef kotlinError = Kotlin_ObjCExport_refFromObjC(error, errorHolder.slot()); // TODO: a simple opaque wrapper would be enough.

  RETURN_RESULT_OF(Kotlin_ObjCExport_NSErrorAsExceptionImpl, message, kotlinError);
}

extern "C" void Kotlin_ObjCExport_RethrowNSErrorAsException(id error) {
    ObjHolder holder;
    KRef exception = Kotlin_ObjCExport_NSErrorAsException(error, holder.slot());
    ThrowException(exception);
}

@interface NSError (NSErrorKotlinException)
@end;

@implementation NSError (NSErrorKotlinException)
-(id)kotlinException {
  auto userInfo = self.userInfo;
  return userInfo == nullptr ? nullptr : userInfo[@"KotlinException"];
}
@end;

#endif
