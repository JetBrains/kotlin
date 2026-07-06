#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

_Bool AsyncAbstractBase_abstractGreet__reverse_swift(void * self, void * continuation, void * exception, void * cancellation);

_Bool AsyncAbstractBase_concreteGreet__reverse_swift(void * self, void * continuation, void * exception, void * cancellation);

_Bool AsyncBase_count__reverse_swift(void * self, void * continuation, void * exception, void * cancellation);

_Bool AsyncBase_greet__TypesOfArguments__Swift_String____reverse_swift(void * self, NSString * name, void * continuation, void * exception, void * cancellation);

NSString * AsyncBase_sync__TypesOfArguments__Swift_String____reverse_swift(void * self, NSString * name);

_Bool AsyncGreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(void * self, NSString * name, void * continuation, void * exception, void * cancellation);

_Bool AsyncGreeterBase_salutation__reverse_swift(void * self, void * continuation, void * exception, void * cancellation);

_Bool AsyncGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(void * self, NSString * name, void * continuation, void * exception, void * cancellation);

_Bool AsyncGreeter_salutation__reverse_swift(void * self, void * continuation, void * exception, void * cancellation);

_Bool AsyncAbstractBase_abstractGreet(void * self, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncAbstractBase_concreteGreet(void * self, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncAbstractBase_concreteGreet_direct(void * self, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncBase_count(void * self, _Bool (^continuation)(int32_t), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncBase_count_direct(void * self, _Bool (^continuation)(int32_t), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncBase_greet__TypesOfArguments__Swift_String__(void * self, NSString * name, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncBase_greet__TypesOfArguments__Swift_String___direct(void * self, NSString * name, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncBase_notOpen(void * self, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

NSString * AsyncBase_sync__TypesOfArguments__Swift_String__(void * self, NSString * name);

NSString * AsyncBase_sync__TypesOfArguments__Swift_String___direct(void * self, NSString * name);

_Bool AsyncGreeterBase_greet__TypesOfArguments__Swift_String__(void * self, NSString * name, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncGreeterBase_greet__TypesOfArguments__Swift_String___direct(void * self, NSString * name, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncGreeterBase_salutation(void * self, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncGreeterBase_salutation_direct(void * self, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncGreeter_greet__TypesOfArguments__Swift_String__(void * self, NSString * name, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

_Bool AsyncGreeter_salutation(void * self, _Bool (^continuation)(NSString *), _Bool (^exception)(void * _Nullable ), void * cancellation);

void * __root___AsyncBase_init_allocate();

_Bool __root___AsyncBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

void * __root___AsyncGreeterBase_init_allocate();

_Bool __root___AsyncGreeterBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

_Bool main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(void * pointerToBlock, NSString * _1);

_Bool main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(void * pointerToBlock, NSError * _Nullable _1);

_Bool main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(void * pointerToBlock, int32_t _1);

NS_ASSUME_NONNULL_END
