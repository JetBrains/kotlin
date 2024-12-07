#import <Foundation/Foundation.h>
#include "with_initializer.h"
@implementation A

@end

@implementation B:A
+(A*)giveC{
  return [[C alloc] init];
}
@end

@implementation C:A
@end

#if 0
int main() {
  [B giveC];
}
#endif
