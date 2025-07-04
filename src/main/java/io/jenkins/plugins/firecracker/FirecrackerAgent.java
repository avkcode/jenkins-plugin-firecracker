package io.jenkins.plugins.firecracker;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirecrackerAgent extends AbstractCloudSlave {
    private static final Logger LOGGER = Logger.getLogger(FirecrackerAgent.class.getName());
    
    private final String ipAddress;
    private final transient FirecrackerVM vm;
    private final String idleTerminationMinutes;
    
    @DataBoundConstructor
    public FirecrackerAgent(
            String name,
            String description,
            String remoteFS,
            int numExecutors,
            Label label,
            List<? extends NodeProperty<?>> nodeProperties,
            String ipAddress,
            FirecrackerVM vm,
            String idleTerminationMinutes) throws Descriptor.FormException, IOException {
        
        super(name, description, remoteFS, numExecutors, Mode.NORMAL, 
              label == null ? null : label.toString(),
              new FirecrackerComputerLauncher(ipAddress),
              new FirecrackerRetentionStrategy(idleTerminationMinutes),
              nodeProperties == null ? Collections.emptyList() : nodeProperties);
        
        this.ipAddress = ipAddress;
        this.vm = vm;
        this.idleTerminationMinutes = idleTerminationMinutes;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public FirecrackerVM getVm() {
        return vm;
    }
    
    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }
    
    @Override
    public AbstractCloudComputer<FirecrackerAgent> createComputer() {
        return new FirecrackerComputer(this);
    }
    
    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Terminating Firecracker VM for node {0}", getNodeName());
        
        try {
            if (vm != null) {
                vm.terminate();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to terminate Firecracker VM for node " + getNodeName(), e);
        }
    }
    
    public static class FirecrackerComputer extends AbstractCloudComputer<FirecrackerAgent> {
        public FirecrackerComputer(FirecrackerAgent agent) {
            super(agent);
        }
        
        @Override
        public FirecrackerAgent getNode() {
            return super.getNode();
        }
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<Node> {
        @Override
        public String getDisplayName() {
            return "Firecracker Agent";
        }
        
        public boolean isInstantiable() {
            return false;
        }
    }
}
