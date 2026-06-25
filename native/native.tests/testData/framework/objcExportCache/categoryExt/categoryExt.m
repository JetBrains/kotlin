#import <Foundation/Foundation.h>
#import <Kt/Kt.h>

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        KtUser *user = [[KtUser alloc] initWithName:@"Bob"];
        NSString *greeting = [KtLibBKt sayHelloReceiver:user];
        if (![greeting isEqualToString:@"Hello, Bob"]) {
            NSLog(@"Expected 'Hello, Bob', got '%@'", greeting);
            return 1;
        }

        NSString *categoryGreeting = [user sayHello];
        if (![categoryGreeting isEqualToString:@"Hello, Bob"]) {
            NSLog(@"Expected 'Hello, Bob', got '%@'", categoryGreeting);
            return 1;
        }

        printf("OK\n");
    }
    return 0;
}
