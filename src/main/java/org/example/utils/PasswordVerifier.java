package org.example.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class PasswordVerifier {
    private PasswordVerifier() {
    }

    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        if (storedHash.equals(rawPassword)) {
            return true;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                resolvePhpCommand(),
                "-r",
                "$password=$argv[1]; $hash=$argv[2]; exit(password_verify($password,$hash)?0:1);",
                "--",
                rawPassword,
                storedHash
        );
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        // Ignore.
                    }
                }
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String resolvePhpCommand() {
        String configured = System.getenv("PHP_BINARY");
        return configured != null && !configured.isBlank() ? configured : "php";
    }
}

