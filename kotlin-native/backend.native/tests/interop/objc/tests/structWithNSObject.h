#import <Foundation/NSString.h>

struct CStructWithNSObjects {
    id any;

    NSString* nsString;
    NSString* _Nonnull nonNullString;
    NSObject* object;

    NSArray* array;
    NSMutableArray* mutableArray;

    NSSet* set;

    NSDictionary* dictionary;
};

