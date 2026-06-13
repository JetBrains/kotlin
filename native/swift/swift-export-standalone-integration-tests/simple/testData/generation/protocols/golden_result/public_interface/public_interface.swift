@_implementationOnly import KotlinBridges_public_interface
import KotlinRuntime
import KotlinRuntimeSupport

public protocol DemoCrossModuleInterface: KotlinRuntime.KotlinBase, public_interface._DemoCrossModuleInterface {
}
@objc(_DemoCrossModuleInterface)
public protocol _DemoCrossModuleInterface {
}
extension public_interface.DemoCrossModuleInterface where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension public_interface.DemoCrossModuleInterface {
}
extension KotlinRuntimeSupport._KotlinExistential: public_interface.DemoCrossModuleInterface where Wrapped : public_interface._DemoCrossModuleInterface {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: public_interface._DemoCrossModuleInterface {
}
