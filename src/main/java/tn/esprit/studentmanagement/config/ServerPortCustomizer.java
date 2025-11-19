package tn.esprit.studentmanagement.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Ensures the embedded web server starts even when the configured port is already in use
 * by automatically falling back to a free TCP port.
 */
@Component
public class ServerPortCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPortCustomizer.class);

    private final Environment environment;

    public ServerPortCustomizer(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        int configuredPort = environment.getProperty("server.port", Integer.class, 8089);
        if (configuredPort <= 0) {
            return;
        }

        if (isPortAvailable(configuredPort)) {
            return;
        }

        int fallbackPort = findAvailableTcpPort();
        factory.setPort(fallbackPort);
        LOGGER.warn("Port {} is already in use. Falling back to available port {}", configuredPort, fallbackPort);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private int findAvailableTcpPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(false);
            return serverSocket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to find an available TCP port", ex);
        }
    }
}

