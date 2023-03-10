package graphql.language

import graphql.TestUtil
import spock.lang.Specification

import static graphql.language.AstPrinter.printAst

class AstSignatureTest extends Specification {

    def query = '''
            query Ouch($secretVariable : String, $otherVariable : Int) {
                fieldZ
                fieldX(password : "hunter2", accountBalance : 200000.23, 
                    avatar : { name : "secretPicture", url : "http://someplace" }
                    favouriteThings : [ "brown", "paper", "packages", "tied", "up", "in", "string" ]
                    likesIceCream : true
                    argToAVariable : $secretVariable
                    anotherArg : $otherVariable
                    )
                fieldY {
                    innerFieldA
                    innerFieldC    
                    innerFieldB
                } 
                ... X   
            }
            
            query Ohh {
                fieldZ
                fieldX
                fieldY {
                    innerFieldA
                    innerFieldC    
                    innerFieldB
                }    
            }
            
            {
                unnamedQuery
                withFields
            }
            
            fragment X on SomeType {
                fieldX(password : "hunter2", accountBalance : 200000.23, 
                    avatar : { name : "secretPicture", url : "http://some place" }
                    favouriteThings : [ "brown", "paper", "packages", "tied", "up", "in", "string" ]
                    likesIceCream : true
                    ) 
            }
            
            type RogueSDLElement {
                field : String
            }
'''
    def "can make a signature for a query"() {

        def expectedQuery = '''query Ouch($var1: String, $var2: Int) {
  fieldX(accountBalance: 0, anotherArg: $var2, argToAVariable: $var1, avatar: {}, favouriteThings: [], likesIceCream: false, password: "")
  fieldY {
    innerFieldA
    innerFieldB
    innerFieldC
  }
  fieldZ
  ...X
}

fragment X on SomeType {
  fieldX(accountBalance: 0, avatar: {}, favouriteThings: [], likesIceCream: false, password: "")
}
'''
        def doc = TestUtil.parseQuery(query)
        when:
        def newDoc = new AstSignature().signatureQuery(doc, "Ouch")
        then:
        newDoc != null
        printAst(newDoc) == expectedQuery
    }

    def "can make a privacy safe document for a query"() {

        def expectedQuery = '''query Ouch($var1: String, $var2: Int) {
  fieldX(accountBalance: 0, anotherArg: $var2, argToAVariable: $var1, avatar: {name : "", url : ""}, favouriteThings: ["", "", "", "", "", "", ""], likesIceCream: false, password: "")
  fieldY {
    innerFieldA
    innerFieldB
    innerFieldC
  }
  fieldZ
  ...X
}

fragment X on SomeType {
  fieldX(accountBalance: 0, avatar: {name : "", url : ""}, favouriteThings: ["", "", "", "", "", "", ""], likesIceCream: false, password: "")
}
'''
        def doc = TestUtil.parseQuery(query)
        when:
        def newDoc = new AstSignature().privacySafeQuery(doc, "Ouch")
        then:
        newDoc != null
        printAst(newDoc) == expectedQuery
    }

    def "can do signature on queries with no name"() {
        def query = """
    {
        allIssues(arg1 : "UGC", arg2 : 666) {
            id
        }
    }"""

        def expectedQuery = """{
  allIssues(arg1: "", arg2: 0) {
    id
  }
}
"""

        def doc = TestUtil.parseQuery(query)
        when:
        def newDoc = new AstSignature().signatureQuery(doc, null)
        then:
        newDoc != null
        printAst(newDoc) == expectedQuery


    }
}
