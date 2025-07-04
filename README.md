# Jenkins Firecracker Plugin

A Jenkins plugin that allows dynamic provisioning of lightweight virtual machines using [AWS Firecracker](https://firecracker-microvm.github.io/) as build agents.

## Overview

This plugin enables Jenkins to create and manage Firecracker microVMs as build agents on demand. Firecracker is a virtualization technology that enables you to implement lightweight virtual machines called microVMs, which provide enhanced security and workload isolation compared to containers, while maintaining minimal resource overhead and fast startup times.

## Features

- Dynamic provisioning of Firecracker microVMs as Jenkins agents
- Template-based VM configuration
- SSH-based agent connection
- Automatic VM termination when idle
- Support for custom VM and kernel images
- Configurable VM resources (memory, vCPUs)

## Requirements

- Linux host with KVM support
- Firecracker binary installed
- VM and kernel images compatible with Firecracker
- Jenkins 2.346.1 or newer

## Configuration

1. Navigate to "Manage Jenkins" > "Manage Nodes and Clouds" > "Configure Clouds"
2. Click "Add a new cloud" and select "Firecracker VM Cloud"
3. Configure the cloud settings:
   - Name: A unique name for this cloud
   - VM Image Path: Path to the Firecracker VM rootfs image
   - Kernel Image Path: Path to the Linux kernel image
   - SSH Credentials: Credentials to connect to the VM
   - Memory Size: Amount of memory in MB
   - VCPU Count: Number of virtual CPUs
   - Instance Cap: Maximum number of instances

4. Add one or more agent templates with specific configurations

## Building the Plugin

```bash
mvn clean package
```

## Testing

### Running Unit Tests
```bash
mvn test
```

### Running Integration Tests
```bash
mvn verify -Pintegration-tests
```

### Running End-to-End Tests
```bash
mvn verify -Pe2e-tests
```

## License

This plugin is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Development Guidelines

### Making Changes with SEARCH/REPLACE Blocks

When submitting changes to this project, please use the following format for your pull requests:

Every *SEARCH/REPLACE block* must use this format:
1. The *FULL* file path alone on a line, verbatim. No bold asterisks, no quotes around it, no escaping of characters, etc.
2. The opening fence and code language, eg: ````java
3. The start of search block: <<<<<<< SEARCH
4. A contiguous chunk of lines to search for in the existing source code
5. The dividing line: =======
6. The lines to replace into the source code
7. The end of the replace block: >>>>>>> REPLACE
8. The closing fence: ````

Important rules:
- Use *quadruple* backticks ```` as fences, not triple backticks
- Every *SEARCH* section must *EXACTLY MATCH* the existing file content
- Keep *SEARCH/REPLACE* blocks concise
- Include enough lines in each SEARCH section to uniquely match each set of lines that need to change
- Break large changes into a series of smaller blocks

This format makes it easier to review and apply changes to the codebase.
