#import <Foundation/NSObject.h>

extern BOOL deallocRetainReleaseDeallocated;

@interface DeallocRetainRelease : NSObject
@end;

extern DeallocRetainRelease* globalDeallocRetainRelease;

@protocol WeakReference
@required
@property (weak) id referent;
@end;

@interface ObjCWeakReference : NSObject <WeakReference>
@property (weak) id referent;
@end;

extern id <WeakReference> weakDeallocLoadWeak;
extern BOOL deallocLoadWeakDeallocated;

@interface DeallocLoadWeak : NSObject
-(void)checkWeak;
@end;
