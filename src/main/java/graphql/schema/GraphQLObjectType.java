package graphql.schema;

import graphql.AssertException;
import graphql.language.ObjectTypeDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertValidName;


public class GraphQLObjectType implements GraphQLType, GraphQLOutputType, GraphQLFieldsContainer, GraphQLCompositeType, GraphQLUnmodifiedType, GraphQLNullableType {


    private final String name;
    private final String description;
    private final Map<String, GraphQLFieldDefinition> fieldDefinitionsByName = new LinkedHashMap<>();
    private final List<TypeOrReference<GraphQLInterfaceType>> tmpInterfaces = new ArrayList<>();
    private List<GraphQLInterfaceType> interfaces;
    private final ObjectTypeDefinition definition;

    public GraphQLObjectType(String name, String description, List<GraphQLFieldDefinition> fieldDefinitions,
                             List<TypeOrReference<GraphQLInterfaceType>> tmpInterfaces) {
        this(name, description, fieldDefinitions, tmpInterfaces, null);
    }

    public GraphQLObjectType(String name, String description, List<GraphQLFieldDefinition> fieldDefinitions,
                             List<TypeOrReference<GraphQLInterfaceType>> tmpInterfaces, ObjectTypeDefinition definition) {
        assertValidName(name);
        assertNotNull(fieldDefinitions, "fieldDefinitions can't be null");
        assertNotNull(tmpInterfaces, "interfaces can't be null");
        assertNotNull(tmpInterfaces, "unresolvedInterfaces can't be null");
        this.name = name;
        this.description = description;
        this.tmpInterfaces.addAll(tmpInterfaces);
        this.definition = definition;
        buildDefinitionMap(fieldDefinitions);
    }

    void replaceTypeReferences(Map<String, GraphQLType> typeMap) {
        this.interfaces = this.tmpInterfaces.stream()
                .map(TypeOrReference::getTypeOrReference)
                .map(type -> (GraphQLInterfaceType) new SchemaUtil().resolveTypeReference(type, typeMap))
                .collect(Collectors.toList());
    }

    private void buildDefinitionMap(List<GraphQLFieldDefinition> fieldDefinitions) {
        for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
            String name = fieldDefinition.getName();
            if (fieldDefinitionsByName.containsKey(name))
                throw new AssertException("field " + name + " redefined");
            fieldDefinitionsByName.put(name, fieldDefinition);
        }
    }

    public GraphQLFieldDefinition getFieldDefinition(String name) {
        return fieldDefinitionsByName.get(name);
    }


    public List<GraphQLFieldDefinition> getFieldDefinitions() {
        return new ArrayList<>(fieldDefinitionsByName.values());
    }


    public List<GraphQLInterfaceType> getInterfaces() {
        if (this.interfaces == null) {
            return this.tmpInterfaces
                    .stream()
                    .filter(TypeOrReference::isType)
                    .map(TypeOrReference::getType)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(interfaces);
    }

    public String getDescription() {
        return description;
    }


    public String getName() {
        return name;
    }

    public ObjectTypeDefinition getDefinition() {
        return definition;
    }

    @Override
    public String toString() {
        return "GraphQLObjectType{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", fieldDefinitionsByName=" + fieldDefinitionsByName +
                ", interfaces=" + interfaces +
                '}';
    }

    public static Builder newObject() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();
        private List<TypeOrReference<GraphQLInterfaceType>> interfaces = new ArrayList<>();
        private ObjectTypeDefinition definition;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder definition(ObjectTypeDefinition definition) {
            this.definition = definition;
            return this;
        }

        public Builder field(GraphQLFieldDefinition fieldDefinition) {
            assertNotNull(fieldDefinition, "fieldDefinition can't be null");
            this.fieldDefinitions.add(fieldDefinition);
            return this;
        }

        /**
         * Take a field builder in a function definition and apply. Can be used in a jdk8 lambda
         * e.g.:
         * <pre>
         *     {@code
         *      field(f -> f.name("fieldName"))
         *     }
         * </pre>
         *
         * @param builderFunction a supplier for the builder impl
         * @return this
         */
        public Builder field(UnaryOperator<GraphQLFieldDefinition.Builder> builderFunction) {
            assertNotNull(builderFunction, "builderFunction can't be null");
            GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition();
            builder = builderFunction.apply(builder);
            return field(builder.build());
        }

        /**
         * Same effect as the field(GraphQLFieldDefinition). Builder.build() is called
         * from within
         *
         * @param builder an un-built/incomplete GraphQLFieldDefinition
         * @return this
         */
        public Builder field(GraphQLFieldDefinition.Builder builder) {
            this.fieldDefinitions.add(builder.build());
            return this;
        }

        public Builder fields(List<GraphQLFieldDefinition> fieldDefinitions) {
            assertNotNull(fieldDefinitions, "fieldDefinitions can't be null");
            this.fieldDefinitions.addAll(fieldDefinitions);
            return this;
        }

        public Builder withInterface(GraphQLInterfaceType interfaceType) {
            assertNotNull(interfaceType, "interfaceType can't be null");
            this.interfaces.add(new TypeOrReference<>(interfaceType));
            return this;
        }

        public Builder withInterface(GraphQLTypeReference reference) {
            assertNotNull(reference, "reference can't be null");
            this.interfaces.add(new TypeOrReference<>(reference));
            return this;
        }

        public Builder withInterfaces(GraphQLInterfaceType... interfaceType) {
            for (GraphQLInterfaceType type : interfaceType) {
                withInterface(type);
            }
            return this;
        }

        public Builder withInterfaces(GraphQLTypeReference... references) {
            for (GraphQLTypeReference reference : references) {
                withInterface(reference);
            }
            return this;
        }

        public GraphQLObjectType build() {
            return new GraphQLObjectType(name, description, fieldDefinitions, interfaces, definition);
        }

    }

}
