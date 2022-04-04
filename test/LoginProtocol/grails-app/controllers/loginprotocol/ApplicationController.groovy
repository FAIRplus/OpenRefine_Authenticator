package loginprotocol

import grails.core.GrailsApplication
import grails.plugin.springsecurity.annotation.Secured
import grails.plugins.*
import static org.springframework.http.HttpStatus.OK

class ApplicationController implements PluginManagerAware {

    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager

    @Secured('permitAll')
    def index() {
        [grailsApplication: grailsApplication, pluginManager: pluginManager]
    }


    @Secured('IS_AUTHENTICATED_FULLY')
    def languages() {
        println "Access Granted"
        respond status: OK, ['EN', 'ES']
    }
}
