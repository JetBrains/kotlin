#import "noAutorelease.h"

@implementation NoAutoreleaseCustomObject
@end;

@interface NoAutoreleaseHelperImpl : NSObject <NoAutoreleaseHelper>
@property (weak) id weakRef;
@end;

int globalBlockResult = 123;

@implementation NoAutoreleaseHelperImpl

-(void)sendObject:(id)obj {
    self.weakRef = obj;
}

-(id)sameObject:(id)obj {
    self.weakRef = obj;
    return obj;
}

-(void)sendCustomObject:(NoAutoreleaseCustomObject*)customObject {
    self.weakRef = customObject;
}

-(NoAutoreleaseCustomObject*)receiveCustomObject {
    NoAutoreleaseCustomObject* result = [NoAutoreleaseCustomObject new];
    self.weakRef = result;
    return result;
}

-(void)sendArray:(NSArray*)array {
    self.weakRef = array;
}

-(NSArray*)receiveArray {
    NSArray* result = @[[NSObject new]];
    self.weakRef = result;
    return result;
}

-(void)sendString:(NSString*)string {
    self.weakRef = string;
}

-(NSString*)receiveString {
    NSString* result = [NSObject new].description;
    self.weakRef = result;
    return result;
}

-(void)sendBlock:(int (^)(void))block {
    self.weakRef = block;
}

-(int (^)(void))receiveBlock {
    int blockResult = globalBlockResult; // To make block capturing.
    int (^result)(void) = ^{ return blockResult; };
    self.weakRef = result;
    return result;
}

-(Boolean)weakIsNull {
    return self.weakRef == nil;
}
@end;

id<NoAutoreleaseHelper> getNoAutoreleaseHelperImpl(void) {
    return [NoAutoreleaseHelperImpl new];
}
