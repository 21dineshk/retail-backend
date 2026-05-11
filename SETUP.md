# retail-backend — Setup Guide

A small Spring Boot service that pretends to be a retail website. It seeds
itself with 10,000 fake products, 8 test users, and ~45 historical orders
when it boots, and exposes a normal REST API plus an OpenAPI document.

This guide assumes **no prior Java experience**. The only thing you need
to install manually is **Java 21**. Everything else (Maven, dependencies,
build tooling) is handled by the included Maven Wrapper (`./mvnw`).

---

## 1. What you get when it's running

| URL | What it is |
|---|---|
| http://localhost:8090 | The API itself |
| http://localhost:8090/swagger-ui.html | Interactive API browser — click any endpoint, fill in params, hit "Execute" |
| http://localhost:8090/v3/api-docs | The OpenAPI JSON document (this is what MCP-Forge ingests) |
| http://localhost:8090/h2 | Web UI for the in-memory database (read-only browsing) |
| http://localhost:8090/actuator/health | Returns `{"status":"UP"}` when the app is healthy |

Test users that exist after boot (you can act as any of them by passing
their numeric `userId` in the URL): alice, bob, carol, david, eva, frank,
grace, henry — all `@example.com`, ids **1–8**.

---

## 2. Install Java 21 (one-time)

Pick your OS. After installing, open a **new** terminal and run
`java -version`. It must report `21.x` or higher.

### macOS

```bash
brew install openjdk@21
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
             /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

(The `ln` step makes the system pick up the new JDK — required on
Apple Silicon.)

### Linux (Debian/Ubuntu)

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
```

(Fedora: `sudo dnf install java-21-openjdk-devel`. Arch: `sudo pacman -S jdk21-openjdk`.)

### Windows

1. Download **Eclipse Temurin 21** (MSI installer) from
   <https://adoptium.net/temurin/releases/?version=21>.
2. Run the installer. **Tick "Set JAVA_HOME variable"** when prompted.
3. Open a **new** PowerShell window (the env vars only apply to new
   sessions) and run `java -version`.

> **Why only Java?** This project ships with a "Maven Wrapper"
> (`mvnw` / `mvnw.cmd`). The first time you build, it downloads the
> right version of Maven into your home directory automatically. You
> never install Maven yourself.

---

## 3. Get the code

```
git clone <this-repo-url> retail-backend
cd retail-backend
```

---

## 4. Build it

The first build downloads ~150 MB of dependencies into `~/.m2`
(`%USERPROFILE%\.m2` on Windows). Subsequent builds reuse the cache and
take seconds.

### macOS / Linux

```bash
./mvnw -DskipTests package
```

### Windows (PowerShell or Command Prompt)

```powershell
mvnw.cmd -DskipTests package
```

You'll see lots of output ending with **`BUILD SUCCESS`**. The build
produces `target/retail-backend-0.1.0.jar` — that's the runnable file.

If you get **`BUILD FAILURE`**, see [section 8](#8-troubleshooting).

---

## 5. Run it

The runnable JAR is the same on every OS — only the way you launch
it in the background differs.

### macOS / Linux — foreground (Ctrl+C to stop)

```bash
java -jar target/retail-backend-0.1.0.jar
```

### macOS / Linux — background

```bash
nohup java -jar target/retail-backend-0.1.0.jar > /tmp/retail-backend.log 2>&1 &
echo "PID: $!"          # remember this number to stop it later
tail -f /tmp/retail-backend.log    # follow the log; Ctrl+C just stops following
```

### Windows — foreground (Ctrl+C to stop)

```powershell
java -jar target\retail-backend-0.1.0.jar
```

### Windows — background

```powershell
$proc = Start-Process -FilePath java `
    -ArgumentList "-jar","target\retail-backend-0.1.0.jar" `
    -RedirectStandardOutput retail-backend.log `
    -RedirectStandardError retail-backend.err `
    -PassThru -WindowStyle Hidden
"PID: $($proc.Id)" | Out-Host
Get-Content retail-backend.log -Wait    # follow the log; Ctrl+C just stops following
```

### When it's up

Within ~5 seconds you'll see these lines in the log:

```
Started RetailBackendApplication in 3.7 seconds
Loaded 10000 products from classpath:seed/products.csv in ~700 ms
Seeded 8 users
Seeded 45 historical orders across 8 users
```

That's the signal it's ready.

---

## 6. Verify it works

Open in your browser (works on every OS):

- <http://localhost:8090/swagger-ui.html> — the API browser
- <http://localhost:8090/actuator/health> — should show `{"status":"UP"}`

Or from the terminal:

### macOS / Linux

```bash
curl http://localhost:8090/actuator/health
curl http://localhost:8090/api/users
curl 'http://localhost:8090/api/products?category=Electronics&maxPrice=50&size=5'
curl http://localhost:8090/api/users/1/orders
```

### Windows (PowerShell)

PowerShell's built-in `curl` is an alias for `Invoke-WebRequest` and uses
different syntax. Either use `curl.exe` (real curl, ships with Windows 10+):

```powershell
curl.exe http://localhost:8090/actuator/health
curl.exe http://localhost:8090/api/users
curl.exe "http://localhost:8090/api/products?category=Electronics&maxPrice=50&size=5"
curl.exe http://localhost:8090/api/users/1/orders
```

…or use the PowerShell-native command:

```powershell
Invoke-RestMethod http://localhost:8090/actuator/health
Invoke-RestMethod http://localhost:8090/api/users
Invoke-RestMethod "http://localhost:8090/api/products?category=Electronics&maxPrice=50&size=5"
Invoke-RestMethod http://localhost:8090/api/users/1/orders
```

### A few endpoints worth trying in the Swagger UI

| Endpoint | What it does |
|---|---|
| `GET /api/products` | Search/browse with pagination, filters, sort |
| `GET /api/products/categories` | All 15 categories with product counts |
| `GET /api/users/{userId}/cart` | Show alice's cart (use `userId=1`) |
| `POST /api/users/{userId}/cart/items` | Add an item: `{"productId": 1, "quantity": 2}` |
| `POST /api/users/{userId}/orders/checkout` | Convert the cart into an order (no payment) |
| `GET /api/users/{userId}/orders` | List a user's recent orders |

---

## 7. Stopping it

### Foreground (any OS)

Press **Ctrl+C** in the terminal where it's running.

### macOS / Linux — background

```bash
kill <PID>                                        # the number you saved above
# Forgot the PID?
lsof -nP -iTCP:8090 -sTCP:LISTEN                  # shows the process owning :8090
pkill -f retail-backend-0.1.0.jar                 # nuclear option
```

### Windows — background

```powershell
Stop-Process -Id <PID>                            # the number you saved above
# Forgot the PID?
Get-NetTCPConnection -LocalPort 8090 |            # shows the process owning :8090
    Select-Object OwningProcess
Get-Process -Name java |                          # all java processes
    Where-Object { $_.MainWindowTitle -eq "" } |
    Stop-Process                                  # nuclear option (kills ALL background java)
```

---

## 8. Troubleshooting

### `command not found: java` (macOS/Linux) / `java is not recognized` (Windows)

Java isn't on your PATH. Open a **new** terminal — env vars set during
install don't apply to already-open terminals.

If still missing: rerun the install in [section 2](#2-install-java-21-one-time).

### `Web server failed to start. Port 8090 was already in use.`

Something else owns 8090. Find it:

| OS | Command |
|---|---|
| macOS / Linux | `lsof -nP -iTCP:8090 -sTCP:LISTEN` |
| Windows | `Get-NetTCPConnection -LocalPort 8090` then `Get-Process -Id <OwningProcess>` |

Stop the offending process, OR change the port:

```yaml
# src/main/resources/application.yml
server:
  port: 8091     # any free port
```

Then rebuild and re-run.

### `BUILD FAILURE` during the first build

- **No internet?** The first build needs to download dependencies.
- **Wrong Java version?** Run `java -version`. It must say **21**. If
  it says 17 or 11, you have an older Java active.
  - macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` then retry.
  - Windows: open `System Properties → Environment Variables` and set
    `JAVA_HOME` to the Temurin 21 install dir, e.g.
    `C:\Program Files\Eclipse Adoptium\jdk-21.x.x`.

### `./mvnw: Permission denied` (macOS/Linux only)

```bash
chmod +x mvnw
```

### "I edited the code but nothing changed"

Spring Boot doesn't auto-reload by default. After any `.java` change,
rebuild and re-run:

```bash
# macOS / Linux
./mvnw -DskipTests package && java -jar target/retail-backend-0.1.0.jar
```

```powershell
# Windows
mvnw.cmd -DskipTests package; java -jar target\retail-backend-0.1.0.jar
```

### Data is gone after I restart

That's expected. The app uses an **in-memory H2 database** that's wiped
on every restart and re-seeded from the CSV. There's no persistence
between runs — by design, so you always start from a known state.

### I want to look at the database directly

1. Make sure the app is running.
2. Open <http://localhost:8090/h2> in a browser.
3. JDBC URL: `jdbc:h2:mem:retail`, user `sa`, leave password blank.
4. Click **Connect**. You can run SQL against the live tables.

---

## 9. Where things live (reference)

```
retail-backend/
├── mvnw                                       ← the Maven Wrapper (Mac/Linux launcher)
├── mvnw.cmd                                   ← Maven Wrapper (Windows launcher)
├── .mvn/wrapper/                              ← wrapper config (which Maven version to download)
├── pom.xml                                    ← Maven build file (dependencies)
├── src/main/
│   ├── java/com/example/retail/
│   │   ├── RetailBackendApplication.java      ← entry point (main method)
│   │   ├── config/                            ← OpenAPI config
│   │   ├── domain/                            ← database table definitions (Product, User, Order, …)
│   │   ├── repository/                        ← database access (auto-generated by Spring)
│   │   ├── service/
│   │   │   └── DataSeeder.java                ← loads the 10k products + test users on boot
│   │   └── web/                               ← REST controllers (the URL handlers)
│   └── resources/
│       ├── application.yml                    ← config (port, DB, etc.)
│       └── seed/products.csv                  ← the 10,000 fake products
└── target/retail-backend-0.1.0.jar            ← created by `./mvnw package`
```
