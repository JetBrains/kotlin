#import <stdio.h>
#import "complexNumbers.h"

@implementation Complex
{
    double re;
    double im;
    NSString *format;
}
- (id)init {
    return [self initWithRe: 0.0 andIm: 0.0];
}

- (id)initWithRe: (double)_re andIm: (double)_im {
    if (self = [super init]) {
        re = _re;
        im = _im;
        format = @"re: %.1lf im: %.1lf";
    }
    return self;
}

+ (Complex *)complexWithRe: (double)re im: (double)im {
    return [[Complex alloc] initWithRe: re andIm: im];
}
- (id<CustomNumber> _Nonnull)add: (id<CustomNumber> _Nonnull)other {
    if ([self isKindOfClass:[Complex class]]) {
        Complex * otherComplex = (Complex *)other;
        return [[Complex alloc] initWithRe: re + otherComplex->re andIm: im + otherComplex->im];
    }
    return NULL;
}

- (id<CustomNumber> _Nonnull)sub: (id<CustomNumber> _Nonnull)other {
    if ([self isKindOfClass:[Complex class]]) {
        Complex * otherComplex = (Complex *)other;
        return [[Complex alloc] initWithRe: re - otherComplex->re andIm: im - otherComplex->im];
    }
    return NULL;
}

- (NSString *)description {
    return [NSString stringWithFormat: format, re, im];
}
@end

@implementation Complex (CategorizedComplex)
- (Complex * _Nonnull)mul: (Complex * _Nonnull)other {
    return [Complex complexWithRe: re * other.re - im * other.im im: re * other.im + im * other.re];
}
- (Complex * _Nonnull)div: (Complex * _Nonnull)other {
    double retRe;
    double retIm;
    double denominator;
    denominator = other.re * other.re + other.im * other.im;
    if (!denominator)
        return nil;
    retRe = (re * other.re + im * other.im) / denominator;
    retIm = (im * other.re - re * other.im) / denominator;
    return [Complex complexWithRe: retRe im: retIm];
}
@end