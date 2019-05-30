package bean;

import org.springframework.beans.factory.annotation.Value;

public class JavaRef {
    @Value("#{ nameJ }") private BeanA valueA;
    @Value("#{ nameK }") private BeanA valueK;
}