package graphql.language;


public class Directive {
    private String name;
    private Value value;

    public Directive() {

    }

    public Directive(String name, Value value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Directive directive = (Directive) o;

        if (name != null ? !name.equals(directive.name) : directive.name != null) return false;
        return !(value != null ? !value.equals(directive.value) : directive.value != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Directive{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
