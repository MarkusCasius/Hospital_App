package com.example.hospimanagmenetapp.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A custom SSLSocketFactory that enables TLSv1.3 and TLSv1.2 on sockets.
 * On older Android versions, TLS 1.3 might be available but not enabled by default.
 * This class ensures that we try to use the most secure protocols available.
 */
public class TLS13SocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory internalSSLSocketFactory;

    public TLS13SocketFactory(SSLSocketFactory delegate) {
        this.internalSSLSocketFactory = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTls13OnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTls13OnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTls13OnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTls13OnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTls13OnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTls13OnSocket(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;

            // Get the list of supported protocols
            String[] supportedProtocols = sslSocket.getSupportedProtocols();
            List<String> wantedProtocols = new ArrayList<>();

            // Add TLSv1.3 and TLSv1.2 if they are supported
            for (String protocol : supportedProtocols) {
                if (protocol.equals("TLSv1.3") || protocol.equals("TLSv1.2")) {
                    wantedProtocols.add(protocol);
                }
            }

            if (!wantedProtocols.isEmpty()) {
                sslSocket.setEnabledProtocols(wantedProtocols.toArray(new String[0]));
            }
        }
        return socket;
    }
}
