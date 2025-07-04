package io.jenkins.plugins.firecracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SSHUtil {
    
    public static boolean checkSSHPort(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
