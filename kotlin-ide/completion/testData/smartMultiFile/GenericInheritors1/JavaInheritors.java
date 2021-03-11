import p.*

class JavaInheritor1 implements KotlinTrait<I1, I2>{}

// is not suitable because type arguments do not match
class JavaInheritor2 implements KotlinTrait<I1, I3>{}
