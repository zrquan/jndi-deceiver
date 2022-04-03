package springboot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class TestApp

fun main() {
    System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase", "true")
    System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true")

    runApplication<TestApp>()
}
