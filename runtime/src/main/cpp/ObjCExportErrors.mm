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
#import "Utils.h"

extern "C" OBJ_GETTER(Kotlin_Throwable_getMessage, KRef throwable);
extern "C" OBJ_GETTER(Kotlin_ObjCExport_getWrappedError, KRef throwable);
extern "C" KBoolean Kotlin_ObjCExport_isUnchecked(KRef exception);

static void printlnMessage(const char* message) {
  konan::consolePrintf("%s\n", message);
}

static const char* uncheckedExceptionMessage =
    "Instances of kotlin.Error, kotlin.RuntimeException and subclasses "
    "aren't propagated from Kotlin to Objective-C/Swift.";

extern "C" RUNTIME_NORETURN void Kotlin_ObjCExport_trapOnUndeclaredException(KRef exception) {
  if (Kotlin_ObjCExport_isUnchecked(exception)) {
    printlnMessage(uncheckedExceptionMessage);
    printlnMessage("Other exceptions can be propagated as NSError if method has or inherits @Throws annotation.");
  } else {
    printlnMessage("Exceptions are propagated from Kotlin to Objective-C/Swift as NSError "
            "only if method has or inherits @Throws annotation");
  }

  TerminateWithUnhandledException(exception);
}

static char kotlinExceptionOriginChar;

extern "C" void Kotlin_ObjCExport_RethrowExceptionAsNSError(KRef exception, id* outError) {
  if (Kotlin_ObjCExport_isUnchecked(exception)) {
    printlnMessage(uncheckedExceptionMessage);
    TerminateWithUnhandledException(exception);
  }

  if (outError == nullptr) {
    return;
  }

  ObjHolder errorHolder, messageHolder;

  KRef error = Kotlin_ObjCExport_getWrappedError(exception, errorHolder.slot());
  if (error != nullptr) {
    *outError = Kotlin_ObjCExport_refToObjC(error);
    return;
  }

  NSMutableDictionary<NSErrorUserInfoKey, id>* userInfo = [[NSMutableDictionary new] autorelease];
  userInfo[@"KotlinException"] = Kotlin_ObjCExport_refToObjC(exception);
  userInfo[@"KotlinExceptionOrigin"] = @(&kotlinExceptionOriginChar); // Support for different Kotlin runtimes loaded.

  KRef message = Kotlin_Throwable_getMessage(exception, messageHolder.slot());
  NSString* description = Kotlin_Interop_CreateNSStringFromKString(message);
  if (description != nullptr) {
    userInfo[NSLocalizedDescriptionKey] = description;
  }

  *outError = [NSError errorWithDomain:@"KotlinException" code:0 userInfo:userInfo];
  return;
}

extern "C" void Kotlin_ObjCExport_RethrowNSErrorAsExceptionImpl(KRef message, KRef error);

extern "C" void Kotlin_ObjCExport_RethrowNSErrorAsException(id error) {
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
        ObjHolder kotlinExceptionHolder;
        ThrowException(Kotlin_ObjCExport_refFromObjC(kotlinException, kotlinExceptionHolder.slot()));
        return;
      }
    }
    description = e.localizedDescription;
  } else {
    description = nullptr;
  }

  ObjHolder messageHolder, errorHolder;
  KRef message = Kotlin_Interop_CreateKStringFromNSString(description, messageHolder.slot());
  KRef kotlinError = Kotlin_ObjCExport_refFromObjC(error, errorHolder.slot()); // TODO: a simple opaque wrapper would be enough.

  Kotlin_ObjCExport_RethrowNSErrorAsExceptionImpl(message, kotlinError);
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
