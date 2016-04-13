import org.springframework.beans.factory.annotation.Autowired

class Bean {
    @Autowired
    fun setProp(autowired<caret>Prop: String) { }
}