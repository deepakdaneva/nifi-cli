# nifi-cli

[![CI](https://github.com/deepakdaneva/nifi-cli/actions/workflows/ci.yml/badge.svg)](https://github.com/deepakdaneva/nifi-cli/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.deepakdaneva.nifi/nifi-cli)](https://central.sonatype.com/artifact/io.github.deepakdaneva.nifi/nifi-cli)
[![License](https://img.shields.io/github/license/deepakdaneva/nifi-cli)](LICENSE)

`nifi-cli` is a command line tool for [Apache NiFi](https://nifi.apache.org). It talks to the NiFi REST API so that
repetitive canvas chores can be scripted instead of done by hand. It is built with [Quarkus](https://quarkus.io) and
[picocli](https://picocli.info).

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Install](#install)
- [Usage](#usage)
  - [Global options](#global-options)
  - [`align`](#align)
  - [`help`](#help)
  - [Exit codes](#exit-codes)
  - [TLS and credentials](#tls-and-credentials)
- [Configuration](#configuration)
- [Build from source](#build-from-source)
- [Compatibility](#compatibility)
- [Contributing](#contributing)
- [Security](#security)
- [Changelog](#changelog)
- [License](#license)
- [Notice](#notice)

## Features

- **Align process groups**: arranges "independent" process groups (canvases that contain nothing but process groups) in
  a tidy grid, recursively down to a chosen depth. Canvases that contain processors, connections, ports, funnels or
  remote process groups are never rearranged, so hand-made flow layouts stay untouched.
- Logs in with username/password, sends the token as `Authorization: Bearer`, and always logs out again (also when a
  command fails or the process is interrupted).
- TLS certificates and host names are verified by default; custom trust stores are supported.
- Clear error messages and [distinct exit codes](#exit-codes) for scripting.

## Requirements

- Java 21 or newer (tested with Java 21 and Java 25).
- An Apache NiFi instance that accepts **username/password login** (`POST /nifi-api/access/token`), i.e. a secured
  (HTTPS) NiFi using the single-user, LDAP or Kerberos login identity provider. NiFi instances without authentication,
  or with only OIDC/SAML/client-certificate login, are not supported.
- The NiFi user needs read access to the process groups to inspect and write access to the process groups to move.

See [Compatibility](#compatibility) for the NiFi versions.

## Install

The CLI is published to Maven Central as a single runnable jar (all dependencies included):

```shell
# replace <version> with the release you want, e.g. 1.0.0
curl -fLO https://repo1.maven.org/maven2/io/github/deepakdaneva/nifi/nifi-cli/<version>/nifi-cli-<version>.jar
java -jar nifi-cli-<version>.jar --help
```

Optionally add a small wrapper script to your `PATH`:

```shell
#!/bin/sh
exec java -jar /opt/nifi-cli/nifi-cli.jar "$@"
```

## Usage

```text
Usage: nifi-cli [-hkV] -p[=<password>] [--truststore-password[=<password>]]
                -l=<location> [--truststore=<file>] -u=<username> [COMMAND]
```

The global options go **before** the command name, the command options after it:

```shell
java -jar nifi-cli.jar -l https://nifi.example.com:8443 -u admin -p align --depth 2
```

### Global options

| Option | Required | Description |
|---|---|---|
| `-l`, `--location=<location>` | yes | NiFi base URL, e.g. `https://nifi.example.com:8443`. Must be `http` or `https` with a host. Any path, query or fragment is ignored; the CLI always calls `<scheme>://<host>[:<port>]/nifi-api`. |
| `-u`, `--username=<username>` | yes | NiFi user name. |
| `-p`, `--password[=<password>]` | yes | Password of the user. Give `-p` without a value to be prompted (input hidden), which keeps the password out of the shell history and process list. |
| `-k`, `--insecure` | no | Disable TLS certificate **and** host name verification. Insecure; only for testing against servers with self-signed certificates. A warning is printed when used. |
| `--truststore=<file>` | no | PKCS12 or JKS trust store used to verify the NiFi server certificate. Without it the JVM's default trusted CAs are used. Ignored when `--insecure` is given. |
| `--truststore-password[=<password>]` | no | Password of the trust store. Give the option without a value to be prompted. |
| `-h`, `--help` | no | Show the help message and exit. |
| `-V`, `--version` | no | Print the version and exit. |

### `align`

Align independent process groups on the canvas in a grid manner.

```text
Usage: nifi-cli align [-hV] [-c=<columns>] [-d=<givenDepth>] [-r=<givenRootPgId>]
```

| Option | Default | Description |
|---|---|---|
| `-d`, `--depth=<givenDepth>` | `5` | Number of levels below the start process group to align. `1` aligns only its direct children, `0` does nothing, `-1` (any negative value) aligns all levels. |
| `-r`, `--rootpgid=<givenRootPgId>` | root ("NiFi Flow") | Id of the process group whose children are aligned first. |
| `-c`, `--columns=<columns>` | `4` | Maximum number of process groups per row; must be at least `1`. |
| `-h`, `--help` | | Show the help message and exit. |
| `-V`, `--version` | | Print the version and exit. |

How it works:

- A canvas is only rearranged if it contains process groups and nothing else (labels are ignored). Otherwise that
  canvas and everything below it is left alone.
- Process groups are placed row by row, ordered by name (case-insensitive, then by id), starting at `(0, 0)`. Each cell
  is 384 x 176 (the size of a process group on the canvas) with a 10 pixel gap.
- Nested process groups are aligned before their parent.
- The command stops at the first error reported by NiFi (for example a revision conflict when someone else edits the
  flow at the same time).

Examples:

```shell
# align the children of the root canvas and five levels below it (defaults), prompting for the password
java -jar nifi-cli.jar -l https://nifi.example.com:8443 -u admin -p align

# align everything below a specific process group, three per row
java -jar nifi-cli.jar -l https://nifi.example.com:8443 -u admin -p align \
  --rootpgid 0189a3f2-0000-1000-ffff-ffffd0a1b2c3 --depth -1 --columns 3

# only the top level, using a custom trust store
java -jar nifi-cli.jar -l https://nifi.example.com:8443 -u admin -p \
  --truststore nifi-ca.p12 --truststore-password align -d 1
```

### `help`

`nifi-cli help` prints the general usage, `nifi-cli help align` the usage of the `align` command. Neither contacts NiFi
nor requires the global options.

### Exit codes

| Code | Meaning |
|---|---|
| `0` | Successful completion. |
| `1` | NiFi request failed (HTTP error, connection or TLS failure) or another unexpected error. |
| `2` | Invalid command line input (missing/invalid option, no command given). |
| `3` | Authentication failed (wrong credentials, or NiFi does not accept username/password login). |

### TLS and credentials

- Certificates and host names are verified by default. For NiFi instances with a certificate from a private CA, pass
  that CA in a trust store with `--truststore` rather than using `--insecure`.
- `--insecure` turns off all TLS verification and must only be used for testing.
- Passwords and access tokens are never logged. The access token only lives in memory and is invalidated with
  `DELETE /nifi-api/access/logout` when the command finishes.

## Configuration

The CLI reads the following properties from the bundled `application.properties`. Each can be overridden with a JVM
system property (`java -D<name>=<value> -jar nifi-cli.jar ...`) or an environment variable (upper case, non
alphanumeric characters replaced by `_`, e.g. `IO_GITHUB_DEEPAKDANEVA_NIFI_CLI_STOP_AT_UNMATCHED`).

| Property | Default | Description |
|---|---|---|
| `io.github.deepakdaneva.nifi.cli.stop-at-unmatched` | `false` | Stop option parsing at the first unmatched argument; the remaining arguments are treated as unmatched. |
| `io.github.deepakdaneva.nifi.cli.unmatched-arguments-allowed` | `true` | Ignore unknown arguments instead of failing with a usage error. Set to `false` for strict parsing. |
| `quarkus.log.category."io.github.deepakdaneva".level` | `INFO` | Log level of the CLI's own messages; `DEBUG` also prints stack traces of failures. |
| `quarkus.log.level` | `OFF` | Log level of the frameworks used by the CLI. |
| `quarkus.rest-client.http2` | `true` | Use HTTP/2 when NiFi supports it. |

Example: `java -Dquarkus.log.category.\"io.github.deepakdaneva\".level=DEBUG -jar nifi-cli.jar ...`

## Build from source

Requirements: JDK 21 or newer. Maven is provided by the wrapper (`./mvnw`, Maven 3.9).

```shell
git clone https://github.com/deepakdaneva/nifi-cli.git
cd nifi-cli

# compile, run the tests, package and check formatting
./mvnw verify

# run the result: target/nifi-cli.jar is a self-contained runnable jar
java -jar target/nifi-cli.jar --help
```

Formatting is checked during `verify`; run `./mvnw spotless:apply` to format the sources (Eclipse formatter settings
in `eclipse-formatter.xml`, license header in `license-header`).

Native executable (requires a GraalVM/Mandrel version supported by Quarkus 3.40 with `native-image`, or a container
runtime for a container build):

```shell
./mvnw -P native package
# or without a local GraalVM:
./mvnw -P native package -Dquarkus.native.container-build=true
./target/nifi-cli --help
```

The native build is provided as is and is not exercised by the automated tests.

Release builds use the `publish` profile (sources, javadoc, GPG signature, upload to Maven Central). Without
credentials it can still be verified locally:

```shell
./mvnw -P publish -Dgpg.skip verify
```

## Compatibility

| nifi-cli | Java | Quarkus | NiFi DTOs compiled against | NiFi server |
|---|---|---|---|---|
| 2.0.x | 21+ | 3.40 (LTS) | 2.12.0 | 1.x and 2.x with username/password login (see below) |
| 1.0.0 | 11+ | 3.4.3 | 1.23.2 | 1.x with username/password login |

The CLI only uses four REST endpoints, all of which exist with the same shape in NiFi 1.x and 2.x:
`POST /access/token`, `DELETE /access/logout`, `GET /flow/process-groups/{id}` and `PUT /process-groups/{id}`. Unknown
fields in NiFi responses are ignored, so newer servers keep working. The automated tests run against a simulated NiFi
REST API, not against a real NiFi instance; please [open an issue](https://github.com/deepakdaneva/nifi-cli/issues) if a
specific NiFi version does not work.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.

## Security

Please report vulnerabilities as described in [SECURITY.md](SECURITY.md) instead of opening a public issue.

## Changelog

Notable changes per release are listed in [CHANGELOG.md](CHANGELOG.md).

## License

This project is licensed under the Apache License 2.0; see [LICENSE](LICENSE).

## Notice

The distributed jar bundles third-party components under their own licenses; see [NOTICE](NOTICE).
