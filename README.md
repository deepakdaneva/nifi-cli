# [nifi-cli](https://github.com/deepakdaneva/nifi-cli)

It's a CLI application developed using [Quarkus](https://quarkus.io) with [Picocli](https://picocli.info) and utilizing Apache NiFi REST APIs internally, all from the command line. Simplify your workflow and boost productivity with this CLI-based tool.

## Table of Contents

- [Usage](#usage)
- [Run](#run)
- [License](#license)
- [Notice](#notice)

## Usage
```
-h, --help      Show this help message and exit.
-l, --location=<location>
                NiFi base url. (i.e. https://somehost.com:8443)
-p, --password=<password>
                Password of the user.
-u, --username=<username>
                Username of the user.
-V, --version   Print version information and exit.
Commands:
  align  Align independent process groups on the canvas in a grid manner.
```

## Run

[nifi-cli](https://github.com/deepakdaneva/nifi-cli) is a normal standalone jar which can be executed as shown below:<br>
`java -jar nifi-cli.jar [-hV] -l=<location> -p=<password> -u=<username> [COMMAND]`

## License

This project is licensed under the Apache License 2.0. For more details, see the [LICENSE](LICENSE) file.

## Notice

This project may use third-party libraries or frameworks that are subject to additional licenses or notices. Please
review the [NOTICE](NOTICE) file for details on these dependencies. These notices must be included in your project as
well, as specified by the respective licenses.