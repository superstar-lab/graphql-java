package graphql.language;


import java.util.ArrayList;
import java.util.List;

public class EnumValue extends AbstractNode<EnumValue> implements Value<EnumValue> {

    private String name;

    public EnumValue(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public boolean isEqualTo(Node o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnumValue that = (EnumValue) o;

        return isEqualTo(this.name,that.name);
    }

    @Override
    public EnumValue deepCopy() {
        return new EnumValue(name);
    }

    @Override
    public String toString() {
        return "EnumValue{" +
                "name='" + name + '\'' +
                '}';
    }
}
