# Keycrate SDK - Java Examples

Simple and full examples for the Keycrate license authentication SDK in Java.

## Prerequisites

-   Java 8 or higher
-   `javac` compiler installed

## Setup

Compile the examples:

```bash
javac SimpleExample.java
javac FullExample.java
```

## Running

Run the compiled class:

```bash
java SimpleExample
java FullExample
```

## Examples

-   **Simple Example** - Basic authentication with license key or username/password, plus registration
-   **Full Example** - Includes HWID detection (Windows/macOS/Linux), detailed error handling, and a post-login menu

## Configuration

Update the host and app ID in the Java file:

```java
private static final String HOST = "https://api.keycrate.dev";
private static final String APP_ID = "YOUR_APP_ID";
```

## HWID Detection

The full example detects hardware ID using:

-   **Windows**: CPU ID and BIOS serial via `wmic`
-   **macOS**: System serial number via `system_profiler`
-   **Linux**: Machine ID from `/etc/machine-id`

All are hashed with SHA256 and truncated to 16 characters.

## Dependencies

-   None - uses only Java built-in libraries (`java.net`, `java.io`, `java.time`)
