package graphql.validation

import graphql.validation.SpecValidationSchema
import graphql.parser.Parser
import graphql.validation.ValidationError
import graphql.validation.Validator
import spock.lang.Requires
import spock.lang.Specification

/**
 * validation examples used in the spec in section 5.1
 * http://facebook.github.io/graphql/#sec-Validation
 * @author dwinsor
 *        
 */
class SpecValidation51Test extends SpecValidationBase {

    def '5.1.1.1 Operation Name Uniqueness Valid'() {
        def query = """
query getDogName {
  dog {
    name
  }
}

query getOwnerName {
  dog {
    owner {
      name
    }
  }
}
"""
        when:
        def validationErrors = validate(query)

        then:
        validationErrors.empty
    }
    
    @Requires({SpecValidationBase.enableStrictValidation})
    def '5.1.1.1 Operation Name Uniqueness Not Valid'() {
        def query = """
query getName {
  dog {
    name
  }
}

query getName {
  dog {
    owner {
      name
    }
  }
}
"""
        when:
        def validationErrors = validate(query)
        //def result = new GraphQL(SpecValidationSchema.specValidationSchema).execute(query)

        then:
        !validationErrors.empty
    }
    
    @Requires({SpecValidationBase.enableStrictValidation})
    def '5.1.1.1 Operation Name Uniqueness Not Valid Different Operations'() {
        def query = """
query dogOperation {
  dog {
    name
  }
}

mutation dogOperation {
  mutateDog {
    id
  }
}
"""
        when:
        def validationErrors = validate(query)

        then:
        !validationErrors.empty
    }
    
    
    def '5.1.2.1 Lone Anonymous Operation Valid'() {
        def query = """
{
  dog {
    name
  }
}
"""
        when:
        def validationErrors = validate(query)

        then:
        validationErrors.empty
    }
    
    
    @Requires({SpecValidationBase.enableStrictValidation})
    def '5.1.2.1 Lone Anonymous Operation Not Valid'() {
        def query = """
{
  dog {
    name
  }
}

query getName {
  dog {
    owner {
      name
    }
  }
}
"""
        when:
        def validationErrors = validate(query)

        then:
        !validationErrors.empty
    }
}
