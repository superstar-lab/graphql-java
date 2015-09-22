package graphql;

import graphql.relay.Relay;
import graphql.schema.*;

import java.util.ArrayList;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class RelaySchema {

    public static Relay relay = new Relay();
    public static GraphQLObjectType StuffType = newObject()
            .name("Stuff")
            .field(newFieldDefinition()
                    .name("id")
                    .type(GraphQLString)
                    .fetchField()
                    .build())

            .build();

    public static GraphQLInterfaceType NodeInterface = relay.nodeInterface(new TypeResolver() {
        @Override
        public GraphQLObjectType getType(Object object) {
            Relay.ResolvedGlobalId resolvedGlobalId = relay.fromGlobalId((String) object);
            //TODO: implement
            return null;
        }
    });

    public static GraphQLObjectType StuffEdgeType = relay.edgeType("Stuff", StuffType, NodeInterface, new ArrayList<GraphQLFieldDefinition>());

    public static GraphQLObjectType StuffConnectionType = relay.connectionType("Stuff", StuffEdgeType, new ArrayList<GraphQLFieldDefinition>());

    public static GraphQLObjectType ThingType = newObject()
            .name("Thing")
            .field(newFieldDefinition()
                    .name("id")
                    .type(GraphQLString)
                    .fetchField()
                    .build())
            .field(newFieldDefinition()
                    .name("stuffs")
                    .type(StuffConnectionType)
                    .build())
            .build();


    public static GraphQLObjectType RelayQueryType = newObject()
            .name("RelayQuery")
            .field(relay.nodeField(NodeInterface, new DataFetcher() {
                @Override
                public Object get(DataFetchingEnvironment environment) {
                    //TODO: implement
                    return null;
                }
            }))
            .field(newFieldDefinition()
                    .name("thing")
                    .type(ThingType)
                    .argument(newArgument()
                            .name("id")
                            .description("id of the thing")
                            .type(new GraphQLNonNull(GraphQLString))
                            .build())
                    .dataFetcher(new DataFetcher() {
                        @Override
                        public Object get(DataFetchingEnvironment environment) {
                            //TODO: implement
                            return null;
                        }
                    })
                    .build())
            .build();


    public static GraphQLSchema Schema = GraphQLSchema.newSchema()
            .query(RelayQueryType)
            .build();
}
