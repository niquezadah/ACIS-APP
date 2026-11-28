import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

/** Diagnóstico de PostgreSQL. Registra solo el estado del cifrado y una lectura de prueba. */
public final class DatabaseTlsProbe {
    private static final String PREFIX = "[acis-db] ";
    private static final Path CA = Path.of("/etc/secrets/supabase-ca.cer");
    private static final byte[] SSL_REQUEST = {0, 0, 0, 8, 4, (byte) 210, 22, 47};

    public static void main(String[] args) {
        try {
            String jdbc = Optional.ofNullable(System.getenv("ACIS_ACCOUNTS_DB"))
                    .orElse(System.getenv("ACIS_DB_URL"));
            if (jdbc == null || !jdbc.startsWith("jdbc:postgresql://")) {
                System.out.println(PREFIX + "URL PostgreSQL ausente o incorrecta");
                return;
            }
            URI address = URI.create(jdbc.substring(5));
            String host = address.getHost();
            if (host == null || address.getUserInfo() != null) {
                System.out.println(PREFIX + "No se pudo identificar el host");
                return;
            }
            int port = address.getPort() == -1 ? 5432 : address.getPort();
            String mode = java.util.Arrays.stream(Optional.ofNullable(address.getRawQuery()).orElse("").split("&"))
                    .filter(value -> value.matches("sslmode=(disable|allow|prefer|require|verify-ca|verify-full)"))
                    .findFirst().orElse("sslmode no identificado");
            System.out.printf("%shost=%s port=%d %s ca=%s java=%s%n",
                    PREFIX, host, port, mode, Files.isReadable(CA), Runtime.version());
            SSLContext tls = buildContext();
            InetAddress[] ips = InetAddress.getAllByName(host);
            for (InetAddress ip : ips) {
                probe(tls, host, ip, port, null);
                probe(tls, host, ip, port, "TLSv1.2");
            }
            probeDriver(jdbc, false);
            probeDriver(jdbc, true);
        } catch (Exception e) {
            // Un mensaje de configuración puede incluir la URL completa. Solo se informa el tipo.
            System.out.println(PREFIX + "error de diagnóstico: " + e.getClass().getSimpleName());
        }
    }

    private static void probeDriver(String jdbc, boolean explicitSni) {
        String label = explicitSni ? "JDBC con SNI explícito" : "JDBC normal";
        String user = Optional.ofNullable(System.getenv("SPRING_DATASOURCE_USERNAME"))
                .orElse(System.getenv("ACIS_DB_USERNAME"));
        String password = Optional.ofNullable(System.getenv("SPRING_DATASOURCE_PASSWORD"))
                .orElse(System.getenv("ACIS_DB_PASSWORD"));
        if (user == null || password == null) {
            System.out.println(PREFIX + label + " faltan variables de acceso");
            return;
        }
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("connectTimeout", "5");
        props.setProperty("socketTimeout", "5");
        if (explicitSni) props.setProperty("sslfactory", ExplicitSniSocketFactory.class.getName());
        try (var connection = DriverManager.getConnection(jdbc, props);
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT 1")) {
            System.out.println(PREFIX + label + (result.next() ? " SELECT 1 correcto" : " sin resultado"));
        } catch (SQLException e) {
            StringBuilder details = new StringBuilder();
            for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
                details.append(" ").append(t.getClass().getSimpleName());
            }
            System.out.println(PREFIX + label + " SQLSTATE=" + e.getSQLState() + details);
        }
    }

    public static final class ExplicitSniSocketFactory extends javax.net.ssl.SSLSocketFactory {
        private final javax.net.ssl.SSLSocketFactory factory;

        public ExplicitSniSocketFactory(Properties props) {
            try {
                if (!"verify-full".equals(props.getProperty("sslmode"))) {
                    throw new IllegalArgumentException("Se requiere verify-full");
                }
                factory = buildContext().getSocketFactory();
            } catch (Exception e) {
                throw new IllegalArgumentException("No se pudo cargar el certificado", e);
            }
        }

        @Override public Socket createSocket(Socket socket, String host, int port, boolean close) throws java.io.IOException {
            var secure = (SSLSocket) factory.createSocket(socket, host, port, close);
            var parameters = secure.getSSLParameters();
            parameters.setServerNames(List.of(new javax.net.ssl.SNIHostName(host)));
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            secure.setSSLParameters(parameters);
            return secure;
        }

        @Override public String[] getDefaultCipherSuites() { return factory.getDefaultCipherSuites(); }
        @Override public String[] getSupportedCipherSuites() { return factory.getSupportedCipherSuites(); }
        @Override public Socket createSocket(String host, int port) throws java.io.IOException { return factory.createSocket(host, port); }
        @Override public Socket createSocket(String host, int port, InetAddress bind, int localPort) throws java.io.IOException {
            return factory.createSocket(host, port, bind, localPort);
        }
        @Override public Socket createSocket(InetAddress host, int port) throws java.io.IOException { return factory.createSocket(host, port); }
        @Override public Socket createSocket(InetAddress host, int port, InetAddress bind, int localPort) throws java.io.IOException {
            return factory.createSocket(host, port, bind, localPort);
        }
    }

    private static SSLContext buildContext() throws Exception {
        KeyStore roots = KeyStore.getInstance(KeyStore.getDefaultType());
        roots.load(null, null);
        try (var in = Files.newInputStream(CA)) {
            var certificates = CertificateFactory.getInstance("X.509").generateCertificates(in);
            int index = 0;
            for (var certificate : certificates) {
                roots.setCertificateEntry("supabase-" + index++, certificate);
            }
        }
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(roots);
        var tls = SSLContext.getInstance("TLS");
        tls.init(null, trust.getTrustManagers(), null);
        return tls;
    }

    private static void probe(SSLContext tls, String host, InetAddress ip, int port, String protocol) {
        String target = PREFIX + ip.getHostAddress() + ":" + port + " "
                + Optional.ofNullable(protocol).orElse("TLS por defecto") + " ";
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 1800);
            socket.setSoTimeout(1800);
            socket.getOutputStream().write(SSL_REQUEST);
            int response = socket.getInputStream().read();
            if (response != 'S') {
                System.out.println(target + "SSLRequest respuesta=" + response);
                return;
            }
            try (var secure = (SSLSocket) tls.getSocketFactory().createSocket(socket, host, port, true)) {
                var parameters = secure.getSSLParameters();
                parameters.setServerNames(List.of(new javax.net.ssl.SNIHostName(host)));
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                if (protocol != null) parameters.setProtocols(new String[]{protocol});
                secure.setSSLParameters(parameters);
                secure.startHandshake();
                System.out.println(target + secure.getSession().getProtocol() + " certificado verificado");
            }
        } catch (Exception e) {
            System.out.println(target + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
