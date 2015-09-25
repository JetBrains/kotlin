import kotlinApi.KotlinClassWithProperties;
import javaApi.JavaClassWithProperties;
import javaApi.JavaClassDerivedFromKotlinClassWithProperties;

import org.jetbrains.annotations.NotNull;

import java.lang.Override;
import java.lang.String;

class A extends KotlinClassWithProperties {
    @NotNull
    @Override
    public String getSomeVar1() {
        return super.getSomeVar1();
    }

    @Override
    public void setSomeVar1(@NotNull String s) {
        super.setSomeVar1(s);
    }

    @NotNull
    @Override
    public String getSomeVar2() {
        return super.getSomeVar2();
    }

    @Override
    public void setSomeVar3(@NotNull String s) {
        super.setSomeVar3(s);
    }

    @NotNull
    @Override
    public String getSomeVar4() {
        return super.getSomeVar4();
    }

    @NotNull
    @Override
    public String getSomeVal() {
        return super.getSomeVal();
    }

    @Override
    public void getSomething1() {
        super.getSomething1();
    }

    @Override
    public void getSomething2() {
        super.getSomething2();
    }

    @Override
    public void setSomething2(int value) {
        super.setSomething2(value);
    }

    @Override
    public void getSomething3() {
        super.getSomething3();
    }

    @Override
    public void setSomething4(int value) {
        super.setSomething4(value);
    }
}

class B extends JavaClassWithProperties {
    @Override
    public int getValue1() {
        return super.getValue1();
    }

    @Override
    public int getValue2() {
        return super.getValue2();
    }

    @Override
    public void setValue2(int value) {
        super.setValue2(value);
    }

    @Override
    public int getValue3() {
        return super.getValue3();
    }

    @Override
    public void setValue4(int value) {
        super.setValue4(value);
    }
}

class C extends A {
    @NotNull
    @Override
    public String getSomeVar1() {
        return super.getSomeVar1();
    }
}

class D extends JavaClassDerivedFromKotlinClassWithProperties {
    @Override
    public String getSomeVar1() { return "a"; }

    @Override
    public void setSomeVar2(String value) { }

}