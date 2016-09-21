/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sshdjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.SshServer;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.Compression;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.compression.CompressionDelayedZlib;
import org.apache.sshd.common.compression.CompressionNone;
import org.apache.sshd.common.compression.CompressionZlib;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sshdjava.overrride.CustomNativeFileSystemFactory;

/**
 *
 * @author alessio
 */
public class SshdJava {

    public static Properties systemProperties;

    static {
        systemProperties = new Properties();
    }

    private static final Logger LOG = LoggerFactory.getLogger(SshdJava.class);

    public static final String CONFIG_FILENAME_PROPERTY = "config.filename";
    public static final String STANDARD_CONFIG_FILENAME = "sshd.properties";

    private static interface DefaultValue {

        public static final String DEFAULT_LOG_CONFIG_FILE_NAME = "log4j.properties";
        public static final int DEFAULT_PORT = 8080;
        public static final String DEFAULT_PEM_FILE_NAME = "ssh_rsa.pem";
        public static final String DEFAULT_ALGORITHM = "RSA";
        public static final int DEFAULT_KEY_LENGTH = 2048;
        public static final String DEFAULT_USERNAME = "username";
        public static final String DEFAULT_PASSWORD = "password";
        public static final boolean DEFAULT_CAN_FORWARD_AGENT = true;
        public static final boolean DEFAULT_CAN_FORWARDX11 = true;
        public static final boolean DEFAULT_CAN_LISTEN = true;
        public static final boolean DEFAULT_CAN_CONNECT = true;

    }

    private static interface ShellCommand {

        public static final String[] UNIX = {"/bin/sh", "-i", "-l"};
        public static final String[] WINDOWS = {"cmd.exe"};
    }

    private static interface ConfigKeys {

        public static final String SSH_PORT = "ssh.port";
        public static final String PEM_FILE_NAME = "ssh.pemfilename";
        public static final String PEM_ALGORITHM = "pem.algorithm";
        public static final String PEM_KEY_LENGTH = "pem.key.length";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String LOG_CONFIG_FILE_NAME = "log4j.configuration";
        public static final String CAN_FORWARD_AGENT = "can.forward.agent";
        public static final String CAN_FORWARDX11 = "can.forwardX11";
        public static final String CAN_LISTEN = "can.listen";
        public static final String CAN_CONNECT = "can.connect";
    }
    
    
    private static interface ClientParam{
        public static final int BUFFER_PIPE_DIM=524288;
        public static final int CHAR_BUFFER_READ_DIM=2048;
        public static final long PERIOD_CLIENT_MS=300000;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        try {
            SshServer sshd = null;
            loadProperty();
            initLogger();
            while (true) {
                try {
                    sshd = SshServer.setUpDefaultServer();
                    LOG.info("default config eseguita");
                    initPort(sshd);
                    sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>>asList(new CompressionNone.Factory(), new CompressionZlib.Factory(), new CompressionDelayedZlib.Factory()));
                    LOG.debug("configurazione compression factories eseguita");
                    sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));
                    LOG.debug("configurazione SftpSubsystem eseguita");
                    initShellFactory(sshd);
                    LOG.debug("configurazione shell eseguita");
                    sshd.setCommandFactory(new ScpCommandFactory());
                    LOG.debug("configurazione ScpCommandFactory eseguita");
                    initKeys(sshd);
                    LOG.debug("configurazione keys eseguita");
                    initAuthenticator(sshd);
                    LOG.debug("configurazione Authenticator eseguita");
                    initTcpipForwardingFilter(sshd);
                    LOG.debug("configurazione TcpipForwardingFilter eseguita");
                    sshd.setFileSystemFactory(new CustomNativeFileSystemFactory());
                    LOG.debug("configurazione NativeFileSystemFactory eseguita");
                    sshd.start();
                    LOG.info("Server ssh started");
                    waitServer();
                } catch (Throwable ex) {
                    LOG.error(ex.getMessage(), ex);
                    if (sshd != null) {
                        sshd.stop();
                        LOG.error("Server ssh stop");
                    }
                    Thread.sleep(10000);
                }
            }
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private static void initShellFactory(SshServer sshd) {
        if (OsUtils.isUNIX()) {
            LOG.info("Sistema operativo UNIX");
            sshd.setShellFactory(new ProcessShellFactory(ShellCommand.UNIX, EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl, ProcessShellFactory.TtyOptions.ONlCr)));
        } else {
            LOG.info("Sistema operativo Windows");
            sshd.setShellFactory(new ProcessShellFactory(ShellCommand.WINDOWS, EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl, ProcessShellFactory.TtyOptions.ONlCr)));
        }
    }

    private static void initTcpipForwardingFilter(SshServer sshd) {
        sshd.setTcpipForwardingFilter(new ForwardingFilter() {
            public boolean canForwardAgent(Session session) {
                String forwardAgent = getProperty(ConfigKeys.CAN_FORWARD_AGENT);
                LOG.debug("property forwardAgent read = " + forwardAgent);
                if (forwardAgent == null) {
                    LOG.debug("property forwardAgent is null uso valore di default = " + DefaultValue.DEFAULT_CAN_FORWARD_AGENT);
                    return DefaultValue.DEFAULT_CAN_FORWARD_AGENT;
                } else {
                    return !Boolean.FALSE.toString().equalsIgnoreCase(forwardAgent);
                }
            }

            public boolean canForwardX11(Session session) {
                String forwardX11 = getProperty(ConfigKeys.CAN_FORWARDX11);
                LOG.debug("property forwardX11 read = " + forwardX11);
                if (forwardX11 == null) {
                    LOG.debug("property forwardX11 is null uso valore di default = " + DefaultValue.DEFAULT_CAN_FORWARDX11);
                    return DefaultValue.DEFAULT_CAN_FORWARDX11;
                } else {
                    return !Boolean.FALSE.toString().equalsIgnoreCase(forwardX11);
                }
            }

            public boolean canListen(SshdSocketAddress address, Session session) {
                String listen = getProperty(ConfigKeys.CAN_LISTEN);
                LOG.debug("property listen read = " + listen);
                if (listen == null) {
                    LOG.debug("property listen is null uso valore di default = " + DefaultValue.DEFAULT_CAN_LISTEN);
                    return DefaultValue.DEFAULT_CAN_LISTEN;
                } else {
                    return !Boolean.FALSE.toString().equalsIgnoreCase(listen);
                }
            }

            public boolean canConnect(SshdSocketAddress address, Session session) {
                String connect = getProperty(ConfigKeys.CAN_CONNECT);
                LOG.debug("property connect read = " + connect);
                if (connect == null) {
                    LOG.debug("property connect is null uso valore di default = " + DefaultValue.DEFAULT_CAN_CONNECT);
                    return DefaultValue.DEFAULT_CAN_CONNECT;
                } else {
                    return !Boolean.FALSE.toString().equalsIgnoreCase(connect);
                }
            }
        });
    }

    private static void initAuthenticator(SshServer sshd) {
        try {
            String usernameProp = getProperty(ConfigKeys.USERNAME);
            LOG.debug("read property username = " + usernameProp);
            String passwordProp = getProperty(ConfigKeys.PASSWORD);
            LOG.debug("read property password = " + passwordProp);
            if (usernameProp == null || passwordProp == null) {
                LOG.debug("property username or password non valide uso i valori di default");
                usernameProp = DefaultValue.DEFAULT_USERNAME;
                passwordProp = DefaultValue.DEFAULT_PASSWORD;
            }
            final String authUsername = usernameProp;
            final String authPassword = passwordProp;
            LOG.debug(String.format("Username = %s , Password = %s", authUsername, authPassword));
            sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
                @Override
                public boolean authenticate(final String username, final String password, final ServerSession session) {
                    return username.equals(authUsername) && password.equals(authPassword);
                }
            });
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private static void initKeys(SshServer sshd) {
        String pemFile = DefaultValue.DEFAULT_PEM_FILE_NAME;
        String algorithm = DefaultValue.DEFAULT_ALGORITHM;
        int keyLength = DefaultValue.DEFAULT_KEY_LENGTH;
        try {
            String tmpPemFile = getProperty(ConfigKeys.PEM_FILE_NAME);
            LOG.debug("property pemfile read = " + tmpPemFile);
            String tmpAlgorithm = getProperty(ConfigKeys.PEM_ALGORITHM);
            LOG.debug("property Algorithm read = " + tmpAlgorithm);
            String tmpKeyLength = getProperty(ConfigKeys.PEM_KEY_LENGTH);
            LOG.debug("property keyLength read = " + tmpKeyLength);
            int realLength = -1;
            if (tmpPemFile != null && tmpAlgorithm != null && tmpKeyLength != null) {
                try {
                    realLength = Integer.parseInt(tmpKeyLength);
                } catch (Throwable ex) {
                    LOG.error("property key length non valida");
                    LOG.error(ex.getMessage(), ex);
                    realLength = -1;
                }
                if (realLength > 0) {
                    LOG.debug("property valida");
                    pemFile = tmpPemFile;
                    algorithm = tmpAlgorithm;
                    keyLength = realLength;
                } else {
                    LOG.debug("uso property di default per pemfile,algorithm e keyLength");
                }
            } else {
                LOG.debug("uso property di default per pemfile,algorithm e keyLength");
            }
            LOG.info(String.format("pemfile = %s , algorithm = %s , keyLength = %d ", pemFile, algorithm, keyLength));
            sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider(pemFile, algorithm, keyLength));
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }

    }

    private static void initPort(SshServer sshd) {
        sshd.setPort(initPort());
    }

    private static int initPort() {
        int realPort = DefaultValue.DEFAULT_PORT;
        try {
            String port = getProperty(ConfigKeys.SSH_PORT);
            LOG.debug("property port read = " + port);

            if (port != null) {
                try {
                    realPort = Integer.parseInt(port);
                } catch (Throwable ex) {
                    LOG.error(ex.getMessage(), ex);
                    LOG.error("porta non valida uso valore di default");
                    realPort = DefaultValue.DEFAULT_PORT;
                }
            }
            LOG.info("porta ascolto server = " + realPort);

        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return realPort;
    }

    private static void initLogger() {
        try {
            String filename = getProperty(ConfigKeys.LOG_CONFIG_FILE_NAME);
            if (filename == null) {
                System.out.println("path del file di log non trovato uso valore di default");
                filename = DefaultValue.DEFAULT_LOG_CONFIG_FILE_NAME;
            }
            Path fileNamePath = FileSystems.getDefault().getPath(filename).toAbsolutePath();
            System.out.println("path del file log configuration = " + fileNamePath.toAbsolutePath().toString());
            if (Files.exists(fileNamePath) && !Files.isDirectory(fileNamePath) && Files.isReadable(fileNamePath)) {
                PropertyConfigurator.configure(Files.newInputStream(fileNamePath, StandardOpenOption.READ));
                LOG.debug("log configurato");
            } else {
                System.out.println("file per la configurazione del log non trovato");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static void loadProperty() {
        try {
            String filename = System.getProperty(CONFIG_FILENAME_PROPERTY);
            if (filename == null) {
                filename = STANDARD_CONFIG_FILENAME;
                System.out.println("path del file di property non trovato uso valore di default");
            }
            Path configFile = Paths.get(filename);
            System.out.println("path del file property configuration = " + configFile.toAbsolutePath().toString());
            if (Files.exists(configFile) && !Files.isDirectory(configFile) && Files.isReadable(configFile)) {
                systemProperties.load(Files.newBufferedReader(configFile, StandardCharsets.UTF_8));
            } else {
                System.out.println("file per la configurazione delle property non trovato");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static String getProperty(String key) {
        String value = null;
        try {
            value = System.getProperty(key);
            if (value == null) {
                value = systemProperties.getProperty(key);
            }
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return value;
    }

    private static void waitServer() {
        try {
            Thread keepAliveClient = new Thread(new SSHClient());
            keepAliveClient.setName("KeepAliveSSHClientThread");
            keepAliveClient.start();
            keepAliveClient.join();
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private static class SSHClient implements Runnable {

        private static final Logger LOG = LoggerFactory.getLogger(SSHClient.class);

        public void run() {
            try {
                int port = initPort();
                String usernameProp = getProperty(ConfigKeys.USERNAME);
                LOG.debug("read property username = " + usernameProp);
                String passwordProp = getProperty(ConfigKeys.PASSWORD);
                LOG.debug("read property password = " + passwordProp);
                if (usernameProp == null || passwordProp == null) {
                    LOG.debug("property username or password non valide uso i valori di default");
                    usernameProp = DefaultValue.DEFAULT_USERNAME;
                    passwordProp = DefaultValue.DEFAULT_PASSWORD;
                }
                if ((port >= 0) && (usernameProp != null) && (passwordProp != null) && (!usernameProp.trim().isEmpty()) && (!passwordProp.trim().isEmpty())) {
                    clientLoop(usernameProp, port, passwordProp);
                } else {
                    LOG.error("Inizializzazione client fallita parametri non validi port = " + port + " username = " + usernameProp + " password = " + passwordProp);
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }

        private void clientLoop(String usernameProp, int port, String passwordProp) {
            try {
                for (;;) {
                    clientKeepAliveCore(usernameProp, port, passwordProp);
                    Thread.sleep(ClientParam.PERIOD_CLIENT_MS);
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }

        private void clientKeepAliveCore(String usernameProp, int port, String passwordProp) throws InterruptedException, IOException {
            SshClient client = SshClient.setUpDefaultClient();
            client.start();
            ConnectFuture awaitConnect = (ConnectFuture) client.connect(usernameProp, new InetSocketAddress("localhost", port)).await();
            LOG.debug("Client isConnected = " + awaitConnect.isConnected() + " Client is Done" + awaitConnect.isDone());
            ClientSession session = awaitConnect.getSession();
            session.addPasswordIdentity(passwordProp);
            AuthFuture await = (AuthFuture) session.auth().await();

            LOG.debug("Client with username@host:port " + usernameProp + "@localhost" + port + " authentication result = " + await.isSuccess());

            ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_SHELL);

            PipedOutputStream pipedINChannel = new PipedOutputStream();
            PipedOutputStream pipedOUTChannelStreamConnect = new PipedOutputStream();
            PipedInputStream pipedOUTChannel = new PipedInputStream(pipedOUTChannelStreamConnect, ClientParam.BUFFER_PIPE_DIM);

            setStreams(channel, pipedINChannel, pipedOUTChannelStreamConnect);
            channel.open();

            writeCommand(pipedINChannel);

            logResult(pipedOUTChannel);

            closeClient(channel, client);
        }

        private void setStreams(ClientChannel channel, PipedOutputStream pipedINChannel, PipedOutputStream pipedOUTChannelStreamConnect) throws IOException {
            channel.setIn(new PipedInputStream(pipedINChannel, ClientParam.BUFFER_PIPE_DIM));
            channel.setOut(pipedOUTChannelStreamConnect);
            channel.setErr(pipedOUTChannelStreamConnect);
        }

        private void writeCommand(PipedOutputStream pipedINChannel) throws IOException {
            pipedINChannel.write("echo test-command\n".getBytes());
            pipedINChannel.flush();

            pipedINChannel.write("exit\n".getBytes());
            pipedINChannel.flush();
        }

        private void closeClient(ClientChannel channel, SshClient client) {
            channel.waitFor(ClientChannel.CLOSED, 0L);

            channel.close(false);
            client.stop();
        }

        private void logResult(PipedInputStream pipedOUTChannel) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(pipedOUTChannel));
            char[] buffer = new char[ClientParam.CHAR_BUFFER_READ_DIM];
            StringBuffer stringBuffer = new StringBuffer();
            int lenRead = reader.read(buffer);
            while (lenRead > 0) {
                stringBuffer.append(buffer, 0, lenRead);
                lenRead = reader.read(buffer);
            }
            LOG.debug(stringBuffer.toString());
        }
    }
}
