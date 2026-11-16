import com.droiddeck.launcher.core.FedoraGamingPayload;
import java.io.*;
public class ApplyGamingPayload {
    public static void main(String[] args) throws Exception {
        File root = new File(args[1]);
        try (InputStream input = new FileInputStream(args[0])) {
            FedoraGamingPayload.install(input, root, new File(root.getParentFile(), root.getName()+"-staging"), args[2]);
        }
        if (!FedoraGamingPayload.ready(root, args[2])) throw new IOException("Gaming payload not ready");
        System.out.println("Bundled gaming payload verified and installed with application code");
    }
}
