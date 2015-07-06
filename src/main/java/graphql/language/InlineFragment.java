package graphql.language;


import java.util.List;

public class InlineFragment implements Selection {
    private String typeCondition;
    private List<Directive> directives;
    private SelectionSet selectionSet;

    public InlineFragment() {

    }

    public InlineFragment(String typeCondition) {
        this.typeCondition = typeCondition;
    }

    public InlineFragment(String typeCondition, List<Directive> directives, SelectionSet selectionSet) {
        this.typeCondition = typeCondition;
        this.directives = directives;
        this.selectionSet = selectionSet;
    }

    public InlineFragment(String typeCondition, SelectionSet selectionSet) {
        this.typeCondition = typeCondition;
        this.selectionSet = selectionSet;
    }


    public String getTypeCondition() {
        return typeCondition;
    }

    public void setTypeCondition(String typeCondition) {
        this.typeCondition = typeCondition;
    }

    public List<Directive> getDirectives() {
        return directives;
    }

    public void setDirectives(List<Directive> directives) {
        this.directives = directives;
    }

    public SelectionSet getSelectionSet() {
        return selectionSet;
    }

    public void setSelectionSet(SelectionSet selectionSet) {
        this.selectionSet = selectionSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InlineFragment that = (InlineFragment) o;

        if (typeCondition != null ? !typeCondition.equals(that.typeCondition) : that.typeCondition != null)
            return false;
        if (directives != null ? !directives.equals(that.directives) : that.directives != null) return false;
        return !(selectionSet != null ? !selectionSet.equals(that.selectionSet) : that.selectionSet != null);

    }

    @Override
    public int hashCode() {
        int result = typeCondition != null ? typeCondition.hashCode() : 0;
        result = 31 * result + (directives != null ? directives.hashCode() : 0);
        result = 31 * result + (selectionSet != null ? selectionSet.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "InlineFragment{" +
                "typeCondition='" + typeCondition + '\'' +
                ", directives=" + directives +
                ", selectionSet=" + selectionSet +
                '}';
    }
}
