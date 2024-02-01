#import <Foundation/NSObject.h>

// [KT-36067] cinterop tool fails when there is a structure member named Companion

struct EnumFieldMangleStruct {
	enum {Companion, Any} smth;
};
struct EnumFieldMangleStruct enumMangledStruct = { Any };

struct MyStruct {
	int Companion;      // simple clash
	int _Companion;
	int $_Companion;
	int super;
};

struct MyStruct myStruct = {11, 12, 13, 14};

@protocol Proto
@property int Companion;  // clash on implementing
@end

@interface FooMangled : NSObject<Proto>
//- (void) CompanionS;  // mangleSimple does not support this: it may clash after mangling
@end
