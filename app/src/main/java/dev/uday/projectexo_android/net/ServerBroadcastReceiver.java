package dev.uday.projectexo_android.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerBroadcastReceiver implements Runnable {
    private static final int BROADCAST_PORT = 7415;
    private boolean isRunning = true;
    // Using ConcurrentHashMap to avoid ConcurrentModificationException
    public static Map<String, ServerInfo> availableServers = new ConcurrentHashMap<>();
    // Map to track last broadcast time for each server
    private static Map<String, Long> lastBroadcastTime = new ConcurrentHashMap<>();
    // Timeout threshold in milliseconds (4 seconds)
    private static final long SERVER_TIMEOUT = 4000;

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
            byte[] buffer = new byte[90]; // Same size as the broadcaster's data

            System.out.println("Server discovery started - listening for broadcasts on port " + BROADCAST_PORT);

            // Start a separate thread to check for server timeouts
            startTimeoutChecker();

            while (isRunning) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("Received broadcast from " + packet.getAddress() + ":" + packet.getPort());

                // Extract server information
                String ipAddress = new String(packet.getData(), 0, 30).trim();
                String port = new String(packet.getData(), 30, 30).trim();
                String serverName = new String(packet.getData(), 60, 30).trim();

                String serverKey = ipAddress + ":" + port;

                // Store server information
                ServerInfo serverInfo = new ServerInfo(ipAddress, port, serverName);
                availableServers.put(serverKey, serverInfo);

                // Update the last broadcast time
                lastBroadcastTime.put(serverKey, System.currentTimeMillis());

            }
            socket.close();
        } catch (SocketException e) {
            System.err.println("Socket error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }

    private void startTimeoutChecker() {
        Thread timeoutChecker = new Thread(() -> {
            while (isRunning) {
                checkForTimeouts();
                try {
                    // Check every second
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timeoutChecker.setDaemon(true);
        timeoutChecker.start();
    }

    private void checkForTimeouts() {
        long currentTime = System.currentTimeMillis();
        boolean serversRemoved = false;

        // Use iterator to safely remove items during iteration
        Iterator<Map.Entry<String, Long>> iterator = lastBroadcastTime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String serverKey = entry.getKey();
            Long lastTime = entry.getValue();

            if (currentTime - lastTime > SERVER_TIMEOUT) {
                // Remove server that hasn't broadcast in over 4 seconds
                availableServers.remove(serverKey);
                iterator.remove();
                serversRemoved = true;
                System.out.println("Removed inactive server: " + serverKey);
            }
        }
    }

    public void stop() {
        isRunning = false;
    }

    // Inner class to store server information
    public static class ServerInfo {
        private final String ipAddress;
        private final String port;
        private final String serverName;

        public ServerInfo(String ipAddress, String port, String serverName) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.serverName = serverName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String getPort() {
            return port;
        }

        public String getServerName() {
            return serverName;
        }

        @Override
        public String toString() {
            return serverName + " (" + ipAddress + ":" + port + ")";
        }
    }
}