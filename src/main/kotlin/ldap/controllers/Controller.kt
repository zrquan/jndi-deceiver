package ldap.controllers

import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult

interface Controller {
    fun sendResult(result: InMemoryInterceptedSearchResult, base: String)
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LDAPMapping(val uri: Array<String>)
