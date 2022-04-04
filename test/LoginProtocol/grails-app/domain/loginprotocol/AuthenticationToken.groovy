package loginprotocol

class AuthenticationToken {

    String tokenValue
    String username

    Date dateCreated

    static mapping = {
        version false
    }

    static constraints = {
    }
}
