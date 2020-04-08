import java.util.ArrayList;
import java.util.List;

public class Owner {
    public List<String> list = new ArrayList<>();
}

public class Updater {
    public void update(Owner owner) {
        owner.list.add("");
    }
}