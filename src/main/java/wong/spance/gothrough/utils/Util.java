package wong.spance.gothrough.utils;

import org.apache.commons.lang3.ClassUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.util.BitSet;
import java.util.Formatter;

/**
 * Created by spance on 14/6/3.
 */
public abstract class Util {

    protected static final BitSet ASCII_QUERY_CHARS;


    @SuppressWarnings("unchecked")
    public static <T> T createBean(String clazz) throws Exception {
        if (clazz == null)
            throw new IllegalArgumentException("clazz null?");
        Class<T> cls = (Class<T>) ClassUtils.getClass(clazz);
        Constructor<T> constructor;
        try {
            constructor = cls.getConstructor();
        } catch (Exception e) {
            constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
        }
        return constructor.newInstance();
    }

    @SuppressWarnings("unchecked")
    public static <T> T createBean(String clazz, Class<?>[] parameterTypes, Object[] args) throws Exception {
        if (clazz == null)
            throw new IllegalArgumentException("clazz null?");
        Class<T> cls = (Class<T>) ClassUtils.getClass(clazz);
        Constructor<T> constructor;
        try {
            constructor = cls.getConstructor(parameterTypes);
        } catch (Exception e) {
            constructor = cls.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
        }
        return constructor.newInstance(args);
    }

    public static boolean hasHeaderAnyOf(HttpServletRequest request, String... headerNames) {
        for (String headerName : headerNames) {
            if (request.getHeader(headerName) != null)
                return true;
        }
        return false;
    }

    /**
     * Encodes characters in the query or fragment part of the URI.
     * <p/>
     * <p>Unfortunately, an incoming URI sometimes has characters disallowed by the spec.  HttpClient
     * insists that the outgoing proxied request has a valid URI because it uses Java's {@link java.net.URI}.
     * To be more forgiving, we must escape the problematic characters.  See the URI class for the
     * spec.
     *
     * @param in example: name=value&foo=bar#fragment
     */
    public static CharSequence encodeUriQuery(CharSequence in) {
        //Note that I can't simply use URI.java to encode because it will escape pre-existing escaped things.
        StringBuilder outBuf = null;
        Formatter formatter = null;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            boolean escape = true;
            if (c < 128) {
                if (ASCII_QUERY_CHARS.get((int) c)) {
                    escape = false;
                }
            } else if (!Character.isISOControl(c) && !Character.isSpaceChar(c)) {//not-ascii
                escape = false;
            }
            if (!escape) {
                if (outBuf != null)
                    outBuf.append(c);
            } else {
                //escape
                if (outBuf == null) {
                    outBuf = new StringBuilder(in.length() + 5 * 3);
                    outBuf.append(in, 0, i);
                    formatter = new Formatter(outBuf);
                }
                //leading %, 0 padded, width 2, capital hex
                formatter.format("%%%02X", (int) c);//TODO
            }
        }
        return outBuf != null ? outBuf : in;
    }

    static {
        char[] c_unreserved = "_-!.~'()*".toCharArray();//plus alphanum
        char[] c_punct = ",;:$&+=".toCharArray();
        char[] c_reserved = "?/[]@".toCharArray();//plus punct

        ASCII_QUERY_CHARS = new BitSet(128);
        for (char c = 'a'; c <= 'z'; c++) ASCII_QUERY_CHARS.set((int) c);
        for (char c = 'A'; c <= 'Z'; c++) ASCII_QUERY_CHARS.set((int) c);
        for (char c = '0'; c <= '9'; c++) ASCII_QUERY_CHARS.set((int) c);
        for (char c : c_unreserved) ASCII_QUERY_CHARS.set((int) c);
        for (char c : c_punct) ASCII_QUERY_CHARS.set((int) c);
        for (char c : c_reserved) ASCII_QUERY_CHARS.set((int) c);

        ASCII_QUERY_CHARS.set((int) '%');//leave existing percent escapes in place
    }

}
