#import <Foundation/Foundation.h>
#import <Kt/Kt.h>

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        KtGamma *g = [[KtGamma alloc] init];
        if (![[g alphaVal] isEqualToString:@"alpha"] ||
            ![[g betaVal] isEqualToString:@"beta"] ||
            ![[g gammaVal] isEqualToString:@"gamma"]) {
            NSLog(@"Failed multi-level cache test");
            return 1;
        }

        printf("OK\n");
    }
    return 0;
}
