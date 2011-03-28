
import java.lang.StringBuilder;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Formatter;

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RMainLoopCallbacks;

import org.apache.commons.lang3.RandomStringUtils;

class NoopConsole implements RMainLoopCallbacks {
    public void rBusy(Rengine re, int which) {};
    public String rChooseFile(Rengine re, int newFile) {
        return null;
    }
    public void rFlushConsole(Rengine re) {};
    public void rLoadHistory(Rengine re, String filename) {};
    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        return null;
    }
    public void rSaveHistory(Rengine re, String filename) {};
    public void rShowMessage(Rengine re, String message) {};
    public void rWriteConsole(Rengine re, String text, int oType) {};
}

interface A {
    String b(String c, String d);
}

class TestInterfaceProxy {
    static Rengine re;

    public static void main(String[] argv) throws Throwable {
        InvocationHandler handler = new InvocationHandler() {
                public Object invoke(Object proxy,
                                     Method method,
                                     Object[] args) {
                    try {
                        StringBuilder evalArgs = new StringBuilder();
                        for (Object arg: args) {
                            // http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string-in-java
                            String identifier = RandomStringUtils.randomAlphabetic(32);
                            // http://tolstoy.newcastle.edu.au/R/help/04/04/0847.html
                            re.assign(identifier, (String) arg);
                            evalArgs.append(identifier);
                            evalArgs.append(",");
                        };
                        if (evalArgs.length() > 0)
                            evalArgs.deleteCharAt(evalArgs.length() - 1);
                        return re.eval(String.format("%s(%s)",
                                                     method.getName(),
                                                     evalArgs)).asString();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        re.end();
                    }
                    System.out.println(method);
                    return new Object();
                };
            };
        A a = (A) Proxy.newProxyInstance(A.class.getClassLoader(),
                                         new Class[] { A.class },
                                         handler);
        re = new Rengine(new String[] { "--vanilla", "--slave" },
                         false,
                         new NoopConsole());
        if (!re.waitForR()) {
            System.out.println("Couldn't initialize R.");
            System.exit(1);
        }
        re.eval("b <- function(c, d) paste(c, d)");
        assert a.b("hello", "world").equals("hello world");
        re.end();
    }
}
