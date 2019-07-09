//file
import kotlinApi.KotlinTrait;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class C {
    void foo() {
        KotlinTrait t = new KotlinTrait() {
            @Nullable
            @Override
            public String nullableFun() {
                return null;
            }

            @NotNull
            @Override
            public String notNullableFun() {
                return "";
            }

            @Override
            public int nonAbstractFun() {
                return 0;
            }
        };
    }
}
