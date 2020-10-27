#import <Foundation/Foundation.h>

@protocol CustomNumber
@required
- (id<CustomNumber> _Nonnull)add: (id<CustomNumber> _Nonnull)other;
- (id<CustomNumber> _Nonnull)sub: (id<CustomNumber> _Nonnull)other;
@end

@interface Complex : NSObject<CustomNumber>

@property (nonatomic, readonly) double re;
@property (nonatomic, readonly) double im;
@property (nonatomic, readwrite) NSString *format;

- (id)initWithRe: (double)re andIm: (double)im;
+ (Complex *)complexWithRe: (double)re im: (double)im;
@end

@interface Complex (CategorizedComplex)
- (Complex * _Nonnull)mul: (Complex * _Nonnull)other;
- (Complex * _Nonnull)div: (Complex * _Nonnull)other;
@end
