#import <Foundation/Foundation.h>
#import <Kt/Kt.h>

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        NSString *a = [KtUtilsKt utilA];
        if (![a isEqualToString:@"UtilA"]) {
            NSLog(@"Expected 'UtilA', got '%@'", a);
            return 1;
        }

        NSString *b = [KtUtilsKt utilB];
        if (![b isEqualToString:@"UtilB"]) {
            NSLog(@"Expected 'UtilB', got '%@'", b);
            return 1;
        }

        printf("OK\n");
    }
    return 0;
}
