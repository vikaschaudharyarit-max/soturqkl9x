# Publishing SmartDBF to Maven Central

This guide contains the **exact, verified steps** used to publish this library to Maven Central.  
After publishing, anyone can use the library with:

```xml
<dependency>
    <groupId>io.github.vikaschaudharyarit-max</groupId>
    <artifactId>smart-dbf</artifactId>
    <version>1.0.1</version>
</dependency>
```

---

## Prerequisites

- Java 17+
- Maven 3.6+
- Git for Windows installed (includes GPG at `C:\Program Files\Git\usr\bin\gpg.exe`)
- A Sonatype Central account at [central.sonatype.com](https://central.sonatype.com)

---

## Step 1: Create a Sonatype Central account and claim your namespace

1. Go to [central.sonatype.com](https://central.sonatype.com) and sign up / log in.
2. Go to **Account → Namespaces** and claim your namespace.  
   - This project uses **`io.github.vikaschaudharyarit-max`** (GitHub-based namespace).
   - For a GitHub namespace, Central auto-verifies by checking you own the GitHub account.
3. Wait for the namespace to show **Verified** before deploying. Publishing to an unverified namespace will fail.

---

## Step 2: Generate a Central user token

1. In [central.sonatype.com](https://central.sonatype.com), go to **Account → Generate User Token**.
2. Copy the **username** and **password** — they are shown only once.

---

## Step 3: Configure `~/.m2/settings.xml`

Edit (or create) **`C:\Users\<you>\.m2\settings.xml`** with the following content.  
Replace the token username/password with what you copied in Step 2.  
Replace the `gpg.passphrase` value with your actual GPG key passphrase.

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_TOKEN_USERNAME</username>
      <password>YOUR_TOKEN_PASSWORD</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>gpg</activeProfile>
  </activeProfiles>
</settings>
```

> **Why store `gpg.passphrase` here?**  
> The `maven-gpg-plugin` (version 1.6, used in this project) reads the passphrase from Maven properties.  
> Storing it in `settings.xml` means you never need to type it on the command line or set environment variables. It works in every session — PowerShell, Git Bash, or CI.

---

## Step 4: Set up GPG

### Generate a key (if you don't have one)

```bash
gpg --full-generate-key
```

- Choose **RSA and RSA**, 3072 or 4096 bits.
- Set your name and email (same as in `pom.xml` developers section).
- Set a passphrase and remember it — this is the `gpg.passphrase` you put in `settings.xml`.

### List your key ID

```bash
gpg --list-secret-keys --keyid-format LONG
```

Example output:
```
sec   rsa3072/4A1117ACF0155128 2026-03-09 [SC]
      8A43027C56C5542DFA7CB2694A1117ACF0155128
uid                 [ unknown] vikas chaudhary <vikaschaudhary432005@gmail.com>
```

The full fingerprint is `8A43027C56C5542DFA7CB2694A1117ACF0155128`.

### Publish the public key to a keyserver

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys 8A43027C56C5542DFA7CB2694A1117ACF0155128
```

Maven Central verifies your signature against the public key on the keyserver.

---

## Step 5: Import your GPG key on Windows (if key was created on Linux)

If you generated the key on Linux/WSL and want to deploy from Windows, you must import the secret key into the Windows GPG keyring once.

**On Linux** (where the key exists):

```bash
gpg --export-secret-keys --armor 8A43027C56C5542DFA7CB2694A1117ACF0155128 > smartdbf-gpg-key.asc
```

Copy `smartdbf-gpg-key.asc` to your Windows machine (USB, SCP, cloud, etc.).

**On Windows** (in Git Bash or PowerShell):

```bash
gpg --import smartdbf-gpg-key.asc
```

Verify it imported:

```bash
gpg --list-secret-keys --keyid-format LONG
```

> **Security:** Delete `smartdbf-gpg-key.asc` from both machines after importing. This file is already in `.gitignore`.

---

## Step 6: `pom.xml` — final working configuration

The key parts that make signing work reliably on Windows are:

1. **`maven-gpg-plugin` version `1.6`** (not 3.x) — v1.6 uses `--passphrase-fd stdin` which reliably reads `gpg.passphrase` from Maven properties without going through the GPG agent.
2. **`<useAgent>false</useAgent>`** — bypasses the GPG agent entirely, preventing cached-passphrase issues.
3. **`--pinentry-mode loopback`** — required for non-interactive (no TTY) environments like PowerShell.
4. **Windows profile** pointing `gpg.executable` to Git for Windows' `gpg.exe`.

```xml
<properties>
    <gpg.executable>gpg</gpg.executable>
    <gpg.keyname>8A43027C56C5542DFA7CB2694A1117ACF0155128</gpg.keyname>
</properties>

<!-- On Windows, automatically use Git for Windows gpg.exe -->
<profiles>
    <profile>
        <id>windows-gpg</id>
        <activation>
            <os><family>windows</family></os>
        </activation>
        <properties>
            <gpg.executable>C:/Program Files/Git/usr/bin/gpg.exe</gpg.executable>
        </properties>
    </profile>
</profiles>

<build>
    <plugins>
        <!-- GPG signing plugin — version 1.6 is the reliable choice on Windows -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-gpg-plugin</artifactId>
            <version>1.6</version>
            <executions>
                <execution>
                    <id>sign-artifacts</id>
                    <phase>verify</phase>
                    <goals><goal>sign</goal></goals>
                    <configuration>
                        <executable>${gpg.executable}</executable>
                        <keyname>${gpg.keyname}</keyname>
                        <useAgent>false</useAgent>
                        <gpgArguments>
                            <arg>--pinentry-mode</arg>
                            <arg>loopback</arg>
                        </gpgArguments>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- Central Publishing plugin -->
        <plugin>
            <groupId>org.sonatype.central</groupId>
            <artifactId>central-publishing-maven-plugin</artifactId>
            <version>0.10.0</version>
            <extensions>true</extensions>
            <configuration>
                <publishingServerId>central</publishingServerId>
                <autoPublish>true</autoPublish>
                <waitUntil>published</waitUntil>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Step 7: Deploy

Make sure your `settings.xml` is set up (Step 3), then run from **any shell** (PowerShell, Git Bash, CMD):

```powershell
mvn clean deploy
```

That's it — no `$env:MAVEN_GPG_PASSPHRASE`, no `export`, nothing extra. The passphrase is read from `settings.xml` automatically.

Maven will:
1. Compile and run tests
2. Build the main JAR, sources JAR, and Javadoc JAR
3. Sign all 4 artifacts with GPG
4. Bundle and upload to Central
5. Wait until the deployment is published (`waitUntil>published`)

On success you will see:
```
[INFO] Deployment ... was successfully published
Packages
 - https://repo1.maven.org/maven2/io/github/vikaschaudharyarit-max/smart-dbf/1.0.1/
[INFO] BUILD SUCCESS
```

---

## Step 8: After release

- **Tag the release in Git:**
  ```bash
  git tag v1.0.1 && git push origin v1.0.1
  ```
- **Bump to next version:**  
  In `pom.xml`, change `<version>` to e.g. `1.1.0-SNAPSHOT` for next development cycle, then back to `1.1.0` when ready to release.
- **Propagation:** After publishing it can take a few minutes to a couple of hours to appear on [search.maven.org](https://search.maven.org/).

---

## Summary checklist

- [ ] Sonatype Central account created
- [ ] Namespace `io.github.vikaschaudharyarit-max` verified in Central Portal
- [ ] Central user token added to `~/.m2/settings.xml` under `<id>central</id>`
- [ ] GPG key generated, public key sent to `keyserver.ubuntu.com`
- [ ] GPG secret key imported on Windows (if key was created on Linux)
- [ ] `gpg.passphrase` added to `~/.m2/settings.xml` under the `gpg` profile
- [ ] `pom.xml` using `maven-gpg-plugin` version `1.6` with `<useAgent>false</useAgent>`
- [ ] `mvn clean deploy` runs successfully with `BUILD SUCCESS`

---

## Troubleshooting

### "Bad passphrase" — the most common issue on Windows

**Root cause:** The GPG agent on Windows (especially Git for Windows) caches passphrases and intercepts signing requests. When it has a stale or empty cache entry, it returns "Bad passphrase" even if the passphrase in `settings.xml` or the environment variable is correct.

**Fix (permanent):** Use `maven-gpg-plugin` version `1.6` with `<useAgent>false</useAgent>` and store the passphrase in `settings.xml` (Steps 3 and 6 above). This completely bypasses the GPG agent.

**Fix (immediate — if you change nothing else):** Kill the GPG agent to clear its cache, then retry:
```powershell
& "C:/Program Files/Git/usr/bin/gpgconf.exe" --kill gpg-agent
mvn clean deploy
```

**Why `maven-gpg-plugin` version 3.x fails on Windows:**  
Version 3.x writes the passphrase to a temp file and passes its path to GPG. Git for Windows' `gpg-agent.exe` intercepts this, ignores the temp file, and tries to prompt for the passphrase interactively — which fails silently in PowerShell. Version 1.6 uses the simpler `--passphrase-fd 0` (stdin pipe) which works reliably everywhere.

---

### "No secret key"

GPG has no key on the current machine. Either generate one (`gpg --full-generate-key`) or import your key from another machine (Step 5).

---

### "Could not determine gpg version" (Windows)

Maven cannot find `gpg.exe`. The `windows-gpg` profile in `pom.xml` automatically points to Git for Windows' GPG. If you installed GPG differently, set the path explicitly:
```powershell
mvn clean deploy -Dgpg.executable="C:\Program Files (x86)\GnuPG\bin\gpg.exe"
```

---

### "Namespace is not allowed"

Your Central account is not allowed to publish under the `groupId` in `pom.xml`. Go to [Central Portal → Account → Namespaces](https://central.sonatype.com/account), verify the namespace matches your `<groupId>`, and ensure its status is **Verified**.

---

### "PKIX path building failed" / SSL certificate error

The JVM doesn't trust the SSL certificate on `central.sonatype.com` — common behind corporate proxies that do HTTPS inspection.

Import your corporate root CA into the JVM's truststore:
```powershell
# Run PowerShell as Administrator
keytool -importcert -alias corporate-ca -file "C:\path\to\corporate-root-ca.crt" `
  -keystore "C:\path\to\jdk\lib\security\cacerts" -storepass changeit
```

Find your JDK path with `mvn -v`. Or try deploying from a non-corporate network (home WiFi, mobile hotspot) to confirm this is the cause.
