import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class FullExample {
    private static final String HOST = "https://api.keycrate.dev";
    private static final String APP_ID = "YOUR_APP_ID";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Keycrate – Full Demo ===\n");

        String hwid = getHWID();
        System.out.println("Your HWID: " + hwid + "\n");

        Scanner scanner = new Scanner(System.in);

        boolean loggedIn = login(scanner, hwid);
        if (!loggedIn) {
            System.out.println("\nAccess denied – goodbye.");
            scanner.close();
            return;
        }

        System.out.println("\nWelcome! You have access.\n");

        String licenseKey = null;

        while (true) {
            System.out.print("Type 'register' or 'exit': ");
            String cmd = scanner.nextLine().trim().toLowerCase();

            if (cmd.equals("exit")) {
                System.out.println("Bye!");
                break;
            } else if (cmd.equals("register")) {
                register(scanner, licenseKey);
                break;
            } else {
                System.out.println("Invalid command.");
            }
        }

        scanner.close();
    }

    static boolean login(Scanner scanner, String hwid) throws Exception {
        System.out.println("=== Login ===");
        System.out.print("License key (press ENTER for username/password): ");
        String key = scanner.nextLine().trim();

        String response;

        if (key.length() > 0) {
            String payload = String.format(
                    "{\"app_id\":\"%s\",\"license\":\"%s\",\"hwid\":\"%s\"}",
                    APP_ID, key, hwid);
            response = makeRequest("/auth", payload);
        } else {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            if (username.isEmpty() || password.isEmpty()) {
                System.out.println("Both fields required.");
                return false;
            }

            String payload = String.format(
                    "{\"app_id\":\"%s\",\"username\":\"%s\",\"password\":\"%s\",\"hwid\":\"%s\"}",
                    APP_ID, username, password, hwid);
            response = makeRequest("/auth", payload);
        }

        boolean success = response.contains("\"success\":true");

        if (!success) {
            String message = extractField(response, "message");
            printError(message, response);
            return false;
        }

        System.out.println("\nLogin successful!\n");
        return true;
    }

    static void register(Scanner scanner, String license) throws Exception {
        System.out.println("\n=== Register Username & Password ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Can't be empty.");
            return;
        }

        String payload = String.format(
                "{\"app_id\":\"%s\",\"license\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}",
                APP_ID, license, username, password);

        String response = makeRequest("/register", payload);
        boolean success = response.contains("\"success\":true");
        String message = extractField(response, "message");

        String color = success ? "\u001B[32m" : "\u001B[31m";
        String reset = "\u001B[0m";
        String status = success ? "SUCCESS" : "FAILED";

        System.out.printf("\n%s%s: %s%s\n", color, status, message, reset);
    }

    static void printError(String msg, String response) {
        System.out.println("\u001B[31mAuthentication failed: " + msg + "\u001B[0m");

        switch (msg) {
            case "LICENSE_NOT_FOUND":
                System.out.println("License key not found – double-check it.");
                break;
            case "INVALID_USERNAME_OR_PASSWORD":
                System.out.println("Wrong username or password.");
                break;
            case "LICENSE_NOT_ACTIVE":
                System.out.println("License is not active – contact support.");
                break;
            case "DEVICE_ALREADY_REGISTERED_WITH_OTHER_LICENSE":
                System.out.println("This device is already bound to another license.");
                break;
            case "LICENSE_EXPIRED":
                String expiresAt = extractField(response, "expires_at");
                if (!expiresAt.isEmpty()) {
                    try {
                        Instant instant = Instant.parse(expiresAt);
                        System.out.println("License expired on: "
                                + instant.toString().replace("T", " ").substring(0, 19) + " UTC");
                    } catch (Exception e) {
                        System.out.println("License has expired (invalid date format).");
                    }
                } else {
                    System.out.println("License has expired.");
                }
                break;
            case "HWID_MISMATCH":
                System.out.println("HWID does not match the registered device.");
                boolean resetAllowed = response.contains("\"hwid_reset_allowed\":true");
                if (resetAllowed) {
                    String lastReset = extractField(response, "last_hwid_reset_at");
                    String cooldown = extractNumberField(response, "hwid_reset_cooldown");
                    if (!lastReset.isEmpty() && !cooldown.isEmpty()) {
                        try {
                            Instant lastDt = Instant.parse(lastReset);
                            long cd = Long.parseLong(cooldown);
                            long elapsed = System.currentTimeMillis() / 1000 - lastDt.getEpochSecond();
                            long left = cd - elapsed;
                            if (left > 0) {
                                System.out.println("Reset available in " + left + " seconds.");
                            } else {
                                System.out.println("HWID reset is now available.");
                            }
                        } catch (Exception e) {
                            System.out.println("Try resetting HWID (invalid timestamp).");
                        }
                    } else {
                        System.out.println("Try resetting HWID.");
                    }
                } else {
                    System.out.println("HWID reset not allowed.");
                }
                break;
            default:
                System.out.println("Unexpected error: " + msg + ". Contact support.");
                break;
        }
    }

    static String makeRequest(String endpoint, String payload) throws Exception {
        URL url = new URL(HOST + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        InputStream stream = (status == 200) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(stream)) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }

        conn.disconnect();
        return response.toString();
    }

    static String getHWID() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return getHWIDWindows();
        } else if (os.contains("mac")) {
            return getHWIDMac();
        } else if (os.contains("nux")) {
            return getHWIDLinux();
        }

        return "unsupported-platform";
    }

    static String getHWIDWindows() throws Exception {
        StringBuilder parts = new StringBuilder();

        try {
            String cpuId = executeCommand("wmic cpu get ProcessorId /format:list");
            String line = cpuId.split("\n")[0];
            if (line.startsWith("ProcessorId=")) {
                parts.append(line.substring(12).trim()).append("|");
            }
        } catch (Exception e) {
        }

        try {
            String biosSerial = executeCommand("wmic bios get SerialNumber /format:list");
            String line = biosSerial.split("\n")[0];
            if (line.startsWith("SerialNumber=")) {
                parts.append(line.substring(13).trim()).append("|");
            }
        } catch (Exception e) {
        }

        String combined = parts.toString();
        if (combined.endsWith("|")) {
            combined = combined.substring(0, combined.length() - 1);
        }

        return hashString(combined).substring(0, 16);
    }

    static String getHWIDMac() throws Exception {
        StringBuilder parts = new StringBuilder();

        try {
            String output = executeCommand("system_profiler SPHardwareDataType");
            for (String line : output.split("\n")) {
                if (line.contains("Serial Number")) {
                    String serial = line.split(":")[1].trim();
                    parts.append(serial);
                }
            }
        } catch (Exception e) {
        }

        return hashString(parts.toString()).substring(0, 16);
    }

    static String getHWIDLinux() throws Exception {
        StringBuilder parts = new StringBuilder();

        try {
            String machineId = new String(Files.readAllBytes(Paths.get("/etc/machine-id")));
            parts.append(machineId.trim());
        } catch (Exception e) {
        }

        return hashString(parts.toString()).substring(0, 16);
    }

    static String executeCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder output = new StringBuilder();

        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                output.append(scanner.nextLine()).append("\n");
            }
        }

        process.waitFor();
        return output.toString();
    }

    static String hashString(String str) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(str.getBytes("utf-8"));
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    static String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1)
            return "";

        start += pattern.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    static String extractNumberField(String json, String field) {
        String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern);
        if (start == -1)
            return "";

        start += pattern.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return json.substring(start, end);
    }
}