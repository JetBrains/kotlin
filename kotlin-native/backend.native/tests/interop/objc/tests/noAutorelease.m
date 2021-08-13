#import "noAutorelease.h"

@implementation NoAutoreleaseCustomObject
@end;

@interface NoAutoreleaseHelperImpl : NSObject <NoAutoreleaseHelper>
@property ObjCLivenessTracker* objCLivenessTracker;

@property id kotlinObject;
@property NoAutoreleaseCustomObject* objCObject;
@property NSArray* array;
@property NSString* string;
@property int (^block)(void);

@end;

@implementation NoAutoreleaseHelperImpl
-(instancetype)init {
    if (self = [super init]) {
        self.objCObject = [NoAutoreleaseCustomObject new];
        self.array = @[[NSObject new]];
        self.string = [[NSObject new] description];
        self.block = createBlock();
    }
    return self;
}

-(void)sendKotlinObject:(id)kotlinObject {
    [self.objCLivenessTracker add:kotlinObject];
}

-(id)receiveKotlinObject {
    id result = self.kotlinObject;
    [self.objCLivenessTracker add:result];
    return result;
}

-(void)sendObjCObject:(NoAutoreleaseCustomObject*)objCObject {
    [self.objCLivenessTracker add:objCObject];
}

-(NoAutoreleaseCustomObject*)receiveObjCObject {
    NoAutoreleaseCustomObject* result = self.objCObject;
    [self.objCLivenessTracker add:result];
    return result;
}

-(void)sendArray:(NSArray*)array {
    [self.objCLivenessTracker add:array];
}

-(NSArray*)receiveArray {
    NSArray* result = self.array;
    [self.objCLivenessTracker add:result];
    return result;
}

-(void)sendString:(NSString*)string {
    [self.objCLivenessTracker add:string];
}

-(NSString*)receiveString {
    NSString* result = self.string;
    [self.objCLivenessTracker add:result];
    return result;
}

-(void)sendBlock:(int (^)(void))block {
    [self.objCLivenessTracker add:block];
}

-(int (^)(void))receiveBlock {
    int (^result)(void) = self.block;
    [self.objCLivenessTracker add:result];
    return result;
}

@end;

id<NoAutoreleaseHelper> getNoAutoreleaseHelperImpl(ObjCLivenessTracker* objCLivenessTracker) {
    NoAutoreleaseHelperImpl* result = [NoAutoreleaseHelperImpl new];
    result.objCLivenessTracker = objCLivenessTracker;
    return result;
}

int blockResult = 42;

@interface ObjCWeakRef : NSObject
@property (weak) id value;
@end;

@implementation ObjCWeakRef;
@end;

@implementation ObjCLivenessTracker {
    NSMutableArray<ObjCWeakRef*>* weakRefs;
}

-(instancetype)init {
    if (self = [super init]) {
        self->weakRefs = [NSMutableArray new];
    }
    return self;
}

-(void)add:(id)obj {
    ObjCWeakRef* weakRef = [ObjCWeakRef new];
    weakRef.value = obj;
    [weakRefs addObject:weakRef];
}

-(Boolean)isEmpty {
    return [weakRefs count] == 0;
}

-(Boolean)objectsAreAlive {
    for (ObjCWeakRef* weakRef in weakRefs) {
        if (weakRef.value == nil) return NO;
    }
    return YES;
}

-(Boolean)objectsAreDead {
    for (ObjCWeakRef* weakRef in weakRefs) {
        if (weakRef.value != nil) return NO;
    }
    return YES;
}

@end;
