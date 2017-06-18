package graphql.schema.idl

import graphql.TypeResolutionEnvironment
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType
import graphql.schema.PropertyDataFetcher
import graphql.schema.TypeResolver
import spock.lang.Specification

class WiringFactoryTest extends Specification {

    class NamedTypeResolver implements TypeResolver {
        String name

        NamedTypeResolver(String name) {
            this.name = name
        }

        @Override
        GraphQLObjectType getType(TypeResolutionEnvironment env) {
            throw new UnsupportedOperationException("Not implemented")
        }
    }

    class NamedDataFetcher implements DataFetcher {
        String name

        NamedDataFetcher(String name) {
            this.name = name
        }

        @Override
        Object get(DataFetchingEnvironment environment) {
            return name
        }
    }

    class NamedWiringFactory implements WiringFactory {
        String name

        NamedWiringFactory(String name) {
            this.name = name
        }

        @Override
        boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
            return name == environment.getInterfaceTypeDefinition().getName()
        }

        @Override
        TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
            return new NamedTypeResolver(name)
        }

        @Override
        boolean providesTypeResolver(UnionWiringEnvironment environment) {
            return name == environment.getUnionTypeDefinition().getName()
        }

        @Override
        TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
            return new NamedTypeResolver(name)
        }

        @Override
        boolean providesDataFetcher(FieldWiringEnvironment environment) {
            return name == environment.getFieldDefinition().getName()
        }

        @Override
        DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
            return new NamedDataFetcher(name)
        }
    }


    GraphQLSchema generateSchema(String schemaSpec, RuntimeWiring wiring) {
        def typeRegistry = new SchemaParser().parse(schemaSpec)
        def result = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring)
        result
    }


    def "ensure that wiring factory is called to resolve and create data fetchers"() {

        def spec = """             

            schema {
              query: Human
            }

            type Episode {
                name : String
            }
            
            interface Character {
                name: String!
            }
            
            union Cyborg = Human | Droid
            
            type Droid implements Character {
                id: ID!
                name: String!
                friends: [Character]
                appearsIn: [Episode]!
            }
                
            type Human implements Character {
                id: ID!
                name: String!
                friends: [Character]
                appearsIn: [Episode]!
                homePlanet: String
                cyborg: Cyborg
            }
        """



        def combinedWiringFactory = new CombinedWiringFactory([
                new NamedWiringFactory("Character"),
                new NamedWiringFactory("Cyborg"),
                new NamedWiringFactory("friends")])

        def wiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(combinedWiringFactory)
                .build()

        def schema = generateSchema(spec, wiring)

        expect:

        GraphQLInterfaceType characterType = schema.getType("Character") as GraphQLInterfaceType

        def characterTypeResolver = characterType.getTypeResolver() as NamedTypeResolver
        characterTypeResolver.name == "Character"

        GraphQLUnionType unionType = schema.getType("Cyborg") as GraphQLUnionType

        def unionTypeResolver = unionType.getTypeResolver() as NamedTypeResolver
        unionTypeResolver.name == "Cyborg"


        GraphQLObjectType humanType = schema.getType("Human") as GraphQLObjectType

        def friendsDataFetcher = humanType.getFieldDefinition("friends").getDataFetcher() as NamedDataFetcher
        friendsDataFetcher.name == "friends"
    }

    def "ensure field wiring environment makes sense"() {
        def spec = """             

            schema {
              query: Human
            }

            type Human {
                id: ID!
                name: String!
                homePlanet: String
            }
        """

        def wiringFactory = new WiringFactory() {

            @Override
            boolean providesDataFetcher(FieldWiringEnvironment environment) {
                assert ["id", "name", "homePlanet"].contains(environment.fieldDefinition.name)
                assert environment.parentType.name == "Human"
                assert environment.registry.getType("Human").isPresent()
                return true
            }

            @Override
            DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
                assert ["id", "name", "homePlanet"].contains(environment.fieldDefinition.name)
                assert environment.parentType.name == "Human"
                assert environment.registry.getType("Human").isPresent()
                new PropertyDataFetcher(environment.fieldDefinition.name)
            }
        }
        def wiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(wiringFactory)
                .build()

        generateSchema(spec, wiring)

        expect:

        true // assertions in callback
    }
}
