#include <stdint.h>
#import <Foundation/Foundation.h>

@protocol ObjCIndexAccess
- (id)loadObjCField:(int32_t)index;
- (void)storeObjCField:(int32_t)index value:(id)value;
@end

id fun4(id l0);
id fun6(id l0);
id fun8(id l0);
id fun10(id l0);
