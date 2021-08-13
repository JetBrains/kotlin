#import <Foundation/NSArray.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSString.h>

@interface ObjCLivenessTracker : NSObject
-(void)add:(id)obj;
-(Boolean)isEmpty;
-(Boolean)objectsAreAlive;
-(Boolean)objectsAreDead;
@end;

@interface NoAutoreleaseCustomObject : NSObject
@end;

@protocol NoAutoreleaseHelper
@required

@property id kotlinObject;

-(void)sendKotlinObject:(id)kotlinObject;
-(id)receiveKotlinObject;

-(void)sendObjCObject:(NoAutoreleaseCustomObject*)objCObject;
-(NoAutoreleaseCustomObject*)receiveObjCObject;

-(void)sendArray:(NSArray*)array;
-(NSArray*)receiveArray;

-(void)sendString:(NSString*)string;
-(NSString*)receiveString;

-(void)sendBlock:(int (^)(void))block;
-(int (^)(void))receiveBlock;
@end;

id<NoAutoreleaseHelper> getNoAutoreleaseHelperImpl(ObjCLivenessTracker*);

void callSendKotlinObject(id<NoAutoreleaseHelper> helper, id obj, ObjCLivenessTracker* tracker) {
    [helper sendKotlinObject:obj];
    [helper sendKotlinObject:obj];
    [tracker add:obj];
}

void callReceiveKotlinObject(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    [tracker add:[helper receiveKotlinObject]];
    [tracker add:[helper receiveKotlinObject]];
}

void callSendObjCObject(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    NoAutoreleaseCustomObject* obj = [NoAutoreleaseCustomObject new];

    [helper sendObjCObject:obj];
    [helper sendObjCObject:obj];
    [tracker add:obj];
}

void callReceiveObjCObject(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    [tracker add:[helper receiveObjCObject]];
    [tracker add:[helper receiveObjCObject]];
}

void callSendArray(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    NSArray* array = @[[NSObject new]];

    [helper sendArray:array];
    [helper sendArray:array];
    [tracker add:array];
}

void callReceiveArray(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    [tracker add:[helper receiveArray]];
    [tracker add:[helper receiveArray]];
}

void callSendString(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    NSString* string = [[NSObject new] description]; // To make it dynamic.

    [helper sendString:string];
    [helper sendString:string];
    [tracker add:string];
}

void callReceiveString(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    [tracker add:[helper receiveString]];
    [tracker add:[helper receiveString]];
}

extern int blockResult;

int (^createBlock(void))() {
    int localBlockResult = blockResult;
    return ^{ return localBlockResult; }; // Try to make it capturing thus dynamic.
}

void callSendBlock(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    int (^block)(void) = createBlock(); // Try to make it heap-allocated.

    [helper sendBlock:block];
    [helper sendBlock:block];
    [tracker add:block];
}

void callReceiveBlock(id<NoAutoreleaseHelper> helper, ObjCLivenessTracker* tracker) {
    [tracker add:[helper receiveBlock]];
    [tracker add:[helper receiveBlock]];
}
