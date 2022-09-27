#ifdef __OBJC__
#import <Foundation/Foundation.h>
#else
#ifndef FOUNDATION_EXPORT
#if defined(__cplusplus)
#define FOUNDATION_EXPORT extern "C"
#else
#define FOUNDATION_EXPORT extern
#endif
#endif
#endif

#import "pod1.h"

FOUNDATION_EXPORT double pod1VersionNumber;
FOUNDATION_EXPORT const unsigned char pod1VersionString[];
