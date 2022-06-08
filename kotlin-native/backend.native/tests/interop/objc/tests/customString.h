#import <Foundation/NSString.h>

@interface CustomString : NSString
- (instancetype)initWithValue:(int)value;
@property int value;
@end

CustomString* _Nonnull createCustomString(int value) {
    return [[CustomString alloc] initWithValue:value];
}

int getCustomStringValue(CustomString* str) {
    return str.value;
}

extern BOOL customStringDeallocated;