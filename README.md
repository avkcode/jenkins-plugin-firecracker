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

## Build and Development Commands

The Makefile provides several useful targets for development:

```bash
make build       # Build plugin package
make test        # Run unit tests
make docker-run  # Start Jenkins with plugin in Docker
make docker-stop # Stop Jenkins container
make clean       # Clean build artifacts
make run-jenkins # Run Jenkins with plugin mounted (dev mode)
```

## Testing

> **Note:** All test commands are also available via `make test`

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
