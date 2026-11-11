package ru.hubber.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ServerPinger {

    public record PingResult(boolean online, int onlinePlayers, int maxPlayers, String version, long pingMs) {
        public static PingResult offline() {
            return new PingResult(false, 0, 0, "", -1);
        }
    }

    private ServerPinger() {
    }

    /**
     * Queries a Minecraft server using standard Server List Ping (SLP) protocol.
     * Falls back to basic TCP socket connection if packet parsing fails.
     */
    public static PingResult ping(String host, int port, int timeoutMs) {
        long startTime = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // 1. Handshake Packet (Packet ID: 0x00)
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(b);
            writeVarInt(handshake, 0x00);        // packet id
            writeVarInt(handshake, -1);          // protocol version (-1 for status)
            writeString(handshake, host);        // server address
            handshake.writeShort(port);          // server port
            writeVarInt(handshake, 1);           // next state: 1 (status)

            byte[] handshakeBytes = b.toByteArray();
            writeVarInt(out, handshakeBytes.length);
            out.write(handshakeBytes);

            // 2. Status Request Packet (Packet ID: 0x00)
            ByteArrayOutputStream req = new ByteArrayOutputStream();
            DataOutputStream reqOut = new DataOutputStream(req);
            writeVarInt(reqOut, 0x00);           // packet id
            byte[] reqBytes = req.toByteArray();
            writeVarInt(out, reqBytes.length);
            out.write(reqBytes);
            out.flush();

            // 3. Read Status Response
            int packetLength = readVarInt(in);
            int packetId = readVarInt(in);

            long elapsed = System.currentTimeMillis() - startTime;

            if (packetId == 0x00) {
                int strLength = readVarInt(in);
                byte[] strBytes = in.readNBytes(strLength);
                String json = new String(strBytes, StandardCharsets.UTF_8);

                try {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    int online = 0;
                    int max = 0;
                    String version = "";

                    if (obj.has("players")) {
                        JsonObject players = obj.getAsJsonObject("players");
                        if (players.has("online")) {
                            online = players.get("online").getAsInt();
                        }
                        if (players.has("max")) {
                            max = players.get("max").getAsInt();
                        }
                    }

                    if (obj.has("version") && obj.getAsJsonObject("version").has("name")) {
                        version = obj.getAsJsonObject("version").get("name").getAsString();
                    }

                    return new PingResult(true, online, max, version, elapsed);
                } catch (Exception e) {
                    // JSON parsing failed, but socket connected successfully
                    return new PingResult(true, 0, 0, "", elapsed);
                }
            } else {
                // Unexpected packet id, but TCP socket is alive and accepting connections
                return new PingResult(true, 0, 0, "", elapsed);
            }
        } catch (Exception e) {
            // Socket connection refused or timed out -> server is offline / restarting
            return PingResult.offline();
        }
    }

    public static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int maxRead = 0;
        while (true) {
            int b = in.readByte();
            i |= (b & 0x7F) << (maxRead++ * 7);
            if (maxRead > 5) {
                throw new IOException("VarInt too big");
            }
            if ((b & 0x80) != 0x80) {
                break;
            }
        }
        return i;
    }

    public static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.writeByte(value);
                return;
            }
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    public static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
}
