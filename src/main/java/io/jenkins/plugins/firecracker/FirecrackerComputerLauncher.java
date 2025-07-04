package io.jenkins.plugins.firecracker;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirecrackerComputerLauncher extends ComputerLauncher {
    private static final Logger LOGGER = Logger.getLogger(FirecrackerComputerLauncher.class.getName());
    
    private final String ipAddress;
    private transient SSHLauncher launcher;
    
    public FirecrackerComputerLauncher(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    @Override
    public void launch(SlaveComputer computer, TaskListener listener) {
        try {
            if (!(computer instanceof FirecrackerAgent.FirecrackerComputer)) {
                throw new IllegalArgumentException("This launcher only works with FirecrackerComputer");
            }
            
            FirecrackerAgent.FirecrackerComputer firecrackerComputer = (FirecrackerAgent.FirecrackerComputer) computer;
            FirecrackerAgent agent = firecrackerComputer.getNode();
            
            if (agent == null) {
                throw new IllegalStateException("Node for computer " + computer.getName() + " is null");
            }
            
            FirecrackerCloud cloud = findCloud();
            if (cloud == null) {
                throw new IllegalStateException("Cannot find Firecracker cloud");
            }
            
            SSHUserPrivateKey credentials = cloud.getSshCredentials();
            if (credentials == null) {
                throw new IllegalStateException("Cannot find SSH credentials with ID: " + cloud.getCredentialsId());
            }
            
            // Wait for VM to be ready for SSH
            waitForSshReady(ipAddress, listener);
            
            // Create SSH launcher
            launcher = new SSHLauncher(
                    ipAddress,
                    22,
                    credentials.getId(),
                    "",  // jvmOptions
                    "",  // javaPath - use default
                    "",  // prefixStartSlaveCmd
                    "",  // suffixStartSlaveCmd
                    60,  // launchTimeoutSeconds
                    10,  // maxNumRetries
                    5,   // retryWaitTime
                    new NonVerifyingKeyVerificationStrategy());
            
            launcher.launch(computer, listener);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to launch agent", e);
            listener.error("Failed to launch agent: " + e.getMessage());
        }
    }
    
    private FirecrackerCloud findCloud() {
        for (FirecrackerCloud cloud : Jenkins.get().clouds.getAll(FirecrackerCloud.class)) {
            return cloud;  // Return the first one for now
        }
        return null;
    }
    
    private void waitForSshReady(String host, TaskListener listener) throws InterruptedException {
        listener.getLogger().println("Waiting for SSH to become available on " + host);
        
        for (int i = 0; i < 60; i++) {
            try {
                if (SSHUtil.checkSSHPort(host, 22)) {
                    listener.getLogger().println("SSH is available on " + host);
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "SSH not yet available on " + host, e);
            }
            
            TimeUnit.SECONDS.sleep(1);
        }
        
        throw new InterruptedException("Timed out waiting for SSH to become available on " + host);
    }
}
