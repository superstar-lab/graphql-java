package graphql.language;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnumValueDefinition extends AbstractNode {
    private final String name;
    private final List<Directive> directives;

    public EnumValueDefinition(String name) {
        this(name, null);
    }

    public EnumValueDefinition(String name, List<Directive> directives) {
        this.name = name;
        this.directives = (null == directives) ? new ArrayList<>() : directives;
    }

    public String getName() {
        return name;
    }

    public List<Directive> getDirectives() {
        return directives;
    }

    public Map<String, Directive> getDirectivesByName() {
        return NodeUtil.directivesByName(directives);
    }

    public Directive getDirective(String directiveName) {
        return getDirectivesByName().get(directiveName);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        result.addAll(directives);
        return result;
    }

    @Override
    public boolean isEqualTo(Node o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnumValueDefinition that = (EnumValueDefinition) o;

        if (null == name) {
            if (null != that.name) return false;
        } else if (!name.equals(that.name)) {
            return false;
        }

        return true;

    }


    @Override
    public String toString() {
        return "EnumValueDefinition{" +
                "name='" + name + '\'' +
                ", directives=" + directives +
                '}';
    }
}
