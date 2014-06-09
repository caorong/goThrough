import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by spance on 14/6/2.
 */
public class Test {

    private InputStream in;

    public static final byte[] ziped = new byte[]{
            31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 51, 52, 52, 4, 0, 61, 81, 107, 77, 3, 0, 0, 0};

    void gzip() throws IOException {
        in = new ByteArrayInputStream("111".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream zipOut = new GZIPOutputStream(out);
        IOUtils.copy(in, zipOut);
        zipOut.finish();
        System.out.println(Arrays.toString(out.toByteArray()));
    }

    void unzip() throws IOException {
        in = new ByteArrayInputStream(ziped);
        in = new GZIPInputStream(in);
        System.out.println(IOUtils.toString(in));
    }

    public static void main(String[] args) {
        try {
            Test me = new Test();
//            me.test();
            me.gzip();
            me.unzip();

//            System.out.println(IOUtils.toString(new GZIPInputStream(new FileInputStream("D:\\local.txt"))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
