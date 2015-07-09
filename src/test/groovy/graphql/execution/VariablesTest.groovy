package graphql.execution

import graphql.Fixtures
import graphql.Scalars
import graphql.language.Type
import graphql.language.TypeName
import graphql.language.VariableDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import spock.lang.Specification


class ResolverTest extends Specification {

    Resolver variables = new Resolver()


    def "resolves variable value"() {
        given:
        VariableDefinition variableDefinition =new VariableDefinition("name", new TypeName("String"))

        when:
        def resolvedValues = variables.getVariableValues(Fixtures.simpsonsSchema(),[variableDefinition],[name: 'homer'])

        then:
        resolvedValues['name'] == 'homer'

    }
}
