#import <Foundation/NSArray.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSString.h>

@interface NoAutoreleaseCustomObject : NSObject
@end;

@protocol NoAutoreleaseHelper
@required

-(void)sendObject:(id)obj;
-(id)sameObject:(id)obj;

-(void)sendCustomObject:(NoAutoreleaseCustomObject*)customObject;
-(NoAutoreleaseCustomObject*)receiveCustomObject;

-(void)sendArray:(NSArray*)array;
-(NSArray*)receiveArray;

-(void)sendString:(NSString*)string;
-(NSString*)receiveString;

-(void)sendBlock:(int (^)(void))block;
-(int (^)(void))receiveBlock;

-(Boolean)weakIsNull;
@end;

id<NoAutoreleaseHelper> getNoAutoreleaseHelperImpl(void);
