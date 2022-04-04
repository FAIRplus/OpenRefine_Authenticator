grails {
    plugin {
        springsecurity {
            filterChain {
                chainMap = [
                        [pattern: '/api/guest/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
                        [pattern: '/api/**', filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter'],
                        [pattern: '/**', filters: 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter']
                ]
            }
            userLookup {
                userDomainClassName = 'loginprotocol.User'
                authorityJoinClassName = 'loginprotocol.UserRole'
            }
            authority {
                className = 'loginprotocol.Role'
            }
            securityConfigType = "InterceptUrlMap"
            interceptUrlMap = [
                    [pattern: '/',                      access: ['permitAll']],
                    [pattern: '/error',                 access: ['permitAll']],
                    [pattern: '/index',                 access: ['permitAll']],
                    [pattern: '/assets/**',             access: ['permitAll']],
                    [pattern: '/**/js/**',              access: ['permitAll']],
                    [pattern: '/**/css/**',             access: ['permitAll']],
                    [pattern: '/**/images/**',          access: ['permitAll']],
                    [pattern: '/**/favicon.ico',        access: ['permitAll']],
                    [pattern: '/login/**',              access: ['permitAll']],
                    [pattern: '/logout',                access: ['permitAll']],
                    [pattern: '/logout/**',             access: ['permitAll']],
                    [pattern: '/**',                    access: ['isAuthenticated()']],
                    [pattern: '/api/logout',            access: ['isAuthenticated()']],
                    [pattern: '/api/login',             access: ['ROLE_ANONYMOUS']]
            ]

            rest {
                token {
                    validation {
                        enableAnonymousAccess = true
                    }
                }
            }
        }
    }
}

grails.gorm.default.mapping = {
    id generator: 'identity'
}


grails.plugin.springsecurity.rest.login.active = true
grails.plugin.springsecurity.rest.login.endpointUrl = '/api/login'
grails.plugin.springsecurity.rest.login.failureStatusCode = 401
grails.plugin.springsecurity.rest.login.useJsonCredentials = true
grails.plugin.springsecurity.rest.login.usernamePropertyName = 'username'
grails.plugin.springsecurity.rest.login.passwordPropertyName = 'password'
grails.plugin.springsecurity.rest.logout.endpointUrl = '/api/logout'
grails.plugin.springsecurity.rest.token.validation.useBearerToken = true
grails.plugin.springsecurity.rest.token.validation.active=true
//grails.plugin.springsecurity.rest.token.validation.headerName='X-Auth-Token'
grails.plugin.springsecurity.rest.token.validation.endpointUrl='/api/validate'
grails.plugin.springsecurity.rest.token.generation.jwt.algorithm = 'HS256'
grails.plugin.springsecurity.rest.token.storage.useGorm = true
grails.plugin.springsecurity.rest.token.storage.jwt.secret = 'LtmeWQ7v0a1rtOVq6Gdh7kONKkKhqe8d'
grails.plugin.springsecurity.rest.token.storage.jwt.expiration = 3600
grails.plugin.springsecurity.rest.token.storage.gorm.tokenDomainClassName = 'loginprotocol.AuthenticationToken'
grails.plugin.springsecurity.rest.token.storage.gorm.tokenValuePropertyName = 'tokenValue'
grails.plugin.springsecurity.rest.token.storage.gorm.usernamePropertyName = 'username'
grails.plugin.springsecurity.rest.token.rendering.tokenPropertyName = 'access_token'
grails.plugin.springsecurity.securityConfigType = 'Annotation'
grails.plugin.springsecurity.auth.loginFormUrl = '/api/login'