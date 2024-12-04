#import <Foundation/NSObject.h>

@protocol KT38234_P1
-(int)foo;
@end

@interface KT38234_Base : NSObject <KT38234_P1>
-(int)callFoo;
@end
