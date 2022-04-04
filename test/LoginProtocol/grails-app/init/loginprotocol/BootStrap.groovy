package loginprotocol

class BootStrap {

    def init = { servletContext ->

        User userAdmin = new User(username: 'admin', password: 'password')
        userAdmin.save()
        User userGuest = new User(username: 'guest', password: 'password')
        userGuest.save()
        Role roleAdmin = new Role(authority: 'ROLE_ADMIN')
        roleAdmin.save()
        Role roleGuest = new Role(authority: 'ROLE_GUEST')
        roleGuest.save()
        UserRole.withTransaction { status ->
            UserRole.create(userAdmin, roleAdmin, true)
            UserRole.create(userGuest, roleGuest, true)
        }

        println userAdmin
        println userGuest
        println roleAdmin
        println roleGuest

    }
    def destroy = {
    }
}
