package io.jenkins.plugins.firecracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirecrackerVM {
    private static final Logger LOGGER = Logger.getLogger(FirecrackerVM.class.getName());
    
    private final String id;
    private final String name;
    private final String vmImagePath;
    private final String kernelImagePath;
    private final int memorySize;
    private final int vcpuCount;
    private final String networkInterface;
    private Process firecrackerProcess;
    private String socketPath;
    private String tapDevice;
    
    public FirecrackerVM(
            String name,
            String vmImagePath,
            String kernelImagePath,
            int memorySize,
            int vcpuCount,
            String networkInterface) {
        
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.vmImagePath = vmImagePath;
        this.kernelImagePath = kernelImagePath;
        this.memorySize = memorySize;
        this.vcpuCount = vcpuCount;
        this.networkInterface = networkInterface;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void start() throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Starting Firecracker VM: {0}", name);
        
        // Create socket path
        Path tempDir = Files.createTempDirectory("firecracker-" + id);
        socketPath = tempDir.resolve("firecracker.socket").toString();
        
        // Create tap device for networking
        tapDevice = "tap" + id.substring(0, 8);
        setupNetworking(tapDevice, networkInterface);
        
        // Start Firecracker process
        List<String> command = new ArrayList<>();
        command.add("firecracker");
        command.add("--api-sock");
        command.add(socketPath);
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        firecrackerProcess = pb.start();
        
        // Wait for socket to be available
        waitForSocket(socketPath);
        
        // Configure VM
        configureVM();
        
        LOGGER.log(Level.INFO, "Firecracker VM started: {0}", name);
    }
    
    public void terminate() {
        LOGGER.log(Level.INFO, "Terminating Firecracker VM: {0}", name);
        
        try {
            if (firecrackerProcess != null && firecrackerProcess.isAlive()) {
                firecrackerProcess.destroy();
                firecrackerProcess.waitFor(30, TimeUnit.SECONDS);
                if (firecrackerProcess.isAlive()) {
                    firecrackerProcess.destroyForcibly();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error terminating Firecracker process", e);
        }
        
        try {
            if (tapDevice != null) {
                cleanupNetworking(tapDevice);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cleaning up network", e);
        }
        
        try {
            if (socketPath != null) {
                Files.deleteIfExists(Paths.get(socketPath));
                Files.deleteIfExists(Paths.get(socketPath).getParent());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cleaning up socket", e);
        }
        
        LOGGER.log(Level.INFO, "Firecracker VM terminated: {0}", name);
    }
    
    public String getIpAddress() throws IOException, InterruptedException {
        // Wait for VM to boot and get IP address
        for (int i = 0; i < 60; i++) {
            String ip = getVMIpAddress();
            if (ip != null && !ip.isEmpty()) {
                return ip;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        
        throw new IOException("Failed to get IP address for VM: " + name);
    }
    
    private void setupNetworking(String tapDevice, String hostInterface) throws IOException, InterruptedException {
        // Create tap device
        executeCommand("sudo", "ip", "tuntap", "add", tapDevice, "mode", "tap");
        
        // Set tap device up
        executeCommand("sudo", "ip", "link", "set", tapDevice, "up");
        
        // Add tap device to bridge
        executeCommand("sudo", "ip", "link", "set", tapDevice, "master", hostInterface);
    }
    
    private void cleanupNetworking(String tapDevice) throws IOException, InterruptedException {
        executeCommand("sudo", "ip", "tuntap", "del", tapDevice, "mode", "tap");
    }
    
    private void waitForSocket(String socketPath) throws InterruptedException {
        File socketFile = new File(socketPath);
        for (int i = 0; i < 30; i++) {
            if (socketFile.exists()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        
        throw new InterruptedException("Timed out waiting for Firecracker socket");
    }
    
    private void configureVM() throws IOException, InterruptedException {
        // Configure boot source
        String bootSourceConfig = String.format(
                "{\"kernel_image_path\": \"%s\", \"boot_args\": \"console=ttyS0 reboot=k panic=1 pci=off\"}",
                kernelImagePath);
        
        executeFirecrackerCmd("PUT", "/boot-source", bootSourceConfig);
        
        // Configure root filesystem
        String rootFsConfig = String.format(
                "{\"drive_id\": \"rootfs\", \"path_on_host\": \"%s\", \"is_root_device\": true, \"is_read_only\": false}",
                vmImagePath);
        
        executeFirecrackerCmd("PUT", "/drives/rootfs", rootFsConfig);
        
        // Configure machine resources
        String machineConfig = String.format(
                "{\"vcpu_count\": %d, \"mem_size_mib\": %d, \"ht_enabled\": false}",
                vcpuCount, memorySize);
        
        executeFirecrackerCmd("PUT", "/machine-config", machineConfig);
        
        // Configure network interface
        String networkConfig = String.format(
                "{\"iface_id\": \"eth0\", \"guest_mac\": \"AA:FC:00:00:00:01\", \"host_dev_name\": \"%s\"}",
                tapDevice);
        
        executeFirecrackerCmd("PUT", "/network-interfaces/eth0", networkConfig);
        
        // Start VM
        executeFirecrackerCmd("PUT", "/actions", "{\"action_type\": \"InstanceStart\"}");
    }
    
    private void executeFirecrackerCmd(String method, String path, String body) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("curl");
        command.add("--unix-socket");
        command.add(socketPath);
        command.add("-X");
        command.add(method);
        command.add("-H");
        command.add("Accept: application/json");
        command.add("-H");
        command.add("Content-Type: application/json");
        command.add("-d");
        command.add(body);
        command.add("http://localhost" + path);
        
        executeCommand(command.toArray(new String[0]));
    }
    
    private String getVMIpAddress() throws IOException, InterruptedException {
        // This is a simplified approach - in a real implementation, you would need
        // a more robust way to determine the VM's IP address, possibly by querying
        // the DHCP server or using a predefined IP allocation
        
        ProcessBuilder pb = new ProcessBuilder("sudo", "arp", "-n");
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("AA:FC:00:00:00:01")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 1) {
                        return parts[0];
                    }
                }
            }
        }
        
        process.waitFor();
        return null;
    }
    
    private void executeCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
                
                throw new IOException("Command failed with exit code " + exitCode + ": " + error.toString());
            }
        }
    }
}
