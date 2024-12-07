#import <Foundation/NSObject.h>

// https://youtrack.jetbrains.com/issue/KT-49455

@interface KT49455 : NSObject
@end

__attribute__((external_source_symbol(language="Swift", defined_in="sample",generated_declaration)))
@interface KT49455 (KT49455Ext)
- (int)extensionFunction;
@end

// Just to ensure that unavailable categories don't break anything.
__attribute__((unavailable("unavailableExtensionFunction is unavailable")))
@interface KT49455 (KT49455UnavailableExt)
- (int)unavailableExtensionFunction;
@end
