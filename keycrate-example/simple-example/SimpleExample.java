import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class SimpleExample {
    private static final String HOST = "https://api.keycrate.dev";
    private static final String APP_ID = "YOUR_APP_ID";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Keycrate – Simple Demo ===\n");

        Scanner scanner = new Scanner(System.in);

        System.out.print(" (1) Authenticate   (2) Register   → ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            System.out.print("License key (or ENTER for username): ");
            String key = scanner.nextLine().trim();

            String response;
            if (key.length() > 0) {
                response = authenticate("license", key, "", "");
            } else {
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Password: ");
                String password = scanner.nextLine().trim();
                response = authenticate("credentials", "", username, password);
            }

            printResult(response);
        } else if (choice.equals("2")) {
            System.out.print("License key to bind: ");
            String lic = scanner.nextLine().trim();
            System.out.print("Username: ");
            String user = scanner.nextLine().trim();
            System.out.print("Password: ");
            String pass = scanner.nextLine().trim();

            String response = register(lic, user, pass);
            printResult(response);
        } else {
            System.out.println("Invalid choice – exiting.");
        }

        scanner.close();
    }

    static String authenticate(String type, String license, String username, String password) {
        try {
            String payload;
            if (type.equals("license")) {
                payload = String.format("{\"app_id\":\"%s\",\"license\":\"%s\"}", APP_ID, license);
            } else {
                payload = String.format("{\"app_id\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}",
                        APP_ID, username, password);
            }

            return makeRequest("/auth", payload);
        } catch (Exception e) {
            return "false|Request failed: " + e.getMessage();
        }
    }

    static String register(String license, String username, String password) {
        try {
            String payload = String.format(
                    "{\"app_id\":\"%s\",\"license\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}",
                    APP_ID, license, username, password);

            return makeRequest("/register", payload);
        } catch (Exception e) {
            return "false|Request failed: " + e.getMessage();
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

    static void printResult(String response) {
        boolean success = response.contains("\"success\":true");
        String message = extractField(response, "message");

        String color = success ? "\u001B[32m" : "\u001B[31m";
        String reset = "\u001B[0m";
        String status = success ? "SUCCESS" : "FAILED";

        System.out.printf("\n%s%s: %s%s\n", color, status, message, reset);
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
}