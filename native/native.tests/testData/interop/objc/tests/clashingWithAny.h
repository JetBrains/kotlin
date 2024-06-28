#import <Foundation/NSObject.h>
#import <Foundation/NSString.h>

@interface TestClashingWithAny1 : NSObject
-(NSString*)toString;
-(NSString*)toString_;
-(int)hashCode;
-(BOOL)equals:(id _Nullable)other;
@end

@interface TestClashingWithAny2 : NSObject
-(void)toString;
-(void)hashCode;
-(void)equals:(int)p; // May clash.
@end

@interface TestClashingWithAny3 : NSObject
// Not clashing actually.
-(NSString*)toString:(int)p;
-(int)hashCode:(int)p;
-(BOOL)equals;
@end
