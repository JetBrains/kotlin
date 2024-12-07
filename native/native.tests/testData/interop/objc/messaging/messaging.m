#import "messaging.h"

@implementation PrimitiveTestSubject

+ (int)intFn {
    return 42;
}

+ (float)floatFn {
    return 3.14f;
}

+ (double)doubleFn {
    return 3.14;
}

+ (simd_float4)simdFn {
    simd_float4 v;
    v.x = 2;
    v.y = 4;
    v.z = 5;
    v.w = 8;
    return v;
}

@end

@implementation AggregateTestSubject

+ (SingleFloat)singleFloatFn {
    SingleFloat s;
    s.f = 3.14f;
    return s;
}

+ (SimplePacked)simplePackedFn {
    SimplePacked s;
    s.f1 = '0';
    s.f2 = 111;
    return s;
}

+ (EvenSmallerPacked)evenSmallerPackedFn {
    EvenSmallerPacked s;
    s.x = 'x';
    s.y = 169;
    s.z = 'z';
    return s;
}

+ (HomogeneousSmall)homogeneousSmallFn {
    HomogeneousSmall s;
    s.f1 = 1.0f;
    s.f2 = 2.0f;
    s.f3 = 3.0f;
    s.f4 = 4.0f;
    return s;
}

+ (HomogeneousBig)homogeneousBigFn {
    HomogeneousBig s;
    s.f1 = 1.0f;
    s.f2 = 2.0f;
    s.f3 = 3.0f;
    s.f4 = 4.0f;
    s.f5 = 5.0f;
    s.f6 = 6.0f;
    s.f7 = 7.0f;
    s.f8 = 8.0f;
    return s;
}

+ (GeterogeneousSmall)geterogeneousSmallFn {
    return (GeterogeneousSmall){1, {1, 4, 9, 25}, 3, 4};
}

+ (simd_quatf)simd_quatfFn {
    return (simd_quatf){ {1, 4, 9, 25} };
}

@end