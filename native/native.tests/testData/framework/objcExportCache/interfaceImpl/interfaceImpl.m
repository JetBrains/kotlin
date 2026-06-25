#import <Foundation/Foundation.h>
#import <Kt/Kt.h>

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        id<KtGreeter> greeter = [KtLibBKt createGreeter];
        NSString *message = [greeter greetName:@"World"];
        if (![message isEqualToString:@"Hello, World!"]) {
            NSLog(@"Expected 'Hello, World!', got '%@'", message);
            return 1;
        }

        KtEnglishGreeter *englishGreeter = [[KtEnglishGreeter alloc] init];
        NSString *message2 = [englishGreeter greetName:@"ObjC"];
        if (![message2 isEqualToString:@"Hello, ObjC!"]) {
            NSLog(@"Expected 'Hello, ObjC!', got '%@'", message2);
            return 1;
        }

        printf("OK\n");
    }
    return 0;
}
