package io.jenkins.plugins.firecracker;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirecrackerCloud extends Cloud {
    private static final Logger LOGGER = Logger.getLogger(FirecrackerCloud.class.getName());
    
    private String credentialsId;
    private String vmImagePath;
    private String kernelImagePath;
    private int memorySize = 1024;
    private int vcpuCount = 1;
    private String networkInterface = "eth0";
    private String agentJarUrl;
    private String javaPath = "/usr/bin/java";
    private int instanceCap = 10;
    private List<FirecrackerAgentTemplate> templates = new ArrayList<>();
    
    @DataBoundConstructor
    public FirecrackerCloud(String name) {
        super(name);
    }
    
    public String getCredentialsId() {
        return credentialsId;
    }
    
    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }
    
    public String getVmImagePath() {
        return vmImagePath;
    }
    
    @DataBoundSetter
    public void setVmImagePath(String vmImagePath) {
        this.vmImagePath = vmImagePath;
    }
    
    public String getKernelImagePath() {
        return kernelImagePath;
    }
    
    @DataBoundSetter
    public void setKernelImagePath(String kernelImagePath) {
        this.kernelImagePath = kernelImagePath;
    }
    
    public int getMemorySize() {
        return memorySize;
    }
    
    @DataBoundSetter
    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }
    
    public int getVcpuCount() {
        return vcpuCount;
    }
    
    @DataBoundSetter
    public void setVcpuCount(int vcpuCount) {
        this.vcpuCount = vcpuCount;
    }
    
    public String getNetworkInterface() {
        return networkInterface;
    }
    
    @DataBoundSetter
    public void setNetworkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
    }
    
    public String getAgentJarUrl() {
        return agentJarUrl;
    }
    
    @DataBoundSetter
    public void setAgentJarUrl(String agentJarUrl) {
        this.agentJarUrl = agentJarUrl;
    }
    
    public String getJavaPath() {
        return javaPath;
    }
    
    @DataBoundSetter
    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }
    
    public int getInstanceCap() {
        return instanceCap;
    }
    
    @DataBoundSetter
    public void setInstanceCap(int instanceCap) {
        this.instanceCap = instanceCap;
    }
    
    public List<FirecrackerAgentTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }
    
    @DataBoundSetter
    public void setTemplates(List<FirecrackerAgentTemplate> templates) {
        this.templates = new ArrayList<>(templates);
    }
    
    public SSHUserPrivateKey getSshCredentials() {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        SSHUserPrivateKey.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.emptyList()),
                CredentialsMatchers.withId(credentialsId));
    }
    
    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        List<NodeProvisioner.PlannedNode> nodes = new ArrayList<>();
        
        for (FirecrackerAgentTemplate template : templates) {
            if (label == null || (template.getLabel() != null && template.getLabel().equals(label))) {
                try {
                    while (excessWorkload > 0 && !exceededCapacity()) {
                        LOGGER.log(Level.INFO, "Provisioning Firecracker VM for label: {0}", label);
                        
                        String nodeName = template.createNodeName();
                        nodes.add(new NodeProvisioner.PlannedNode(
                                nodeName,
                                Computer.threadPoolForRemoting.submit(new ProvisioningCallback(template, nodeName)),
                                template.getNumExecutors()));
                        
                        excessWorkload -= template.getNumExecutors();
                    }
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to provision Firecracker VM", e);
                }
            }
        }
        
        return nodes;
    }
    
    private boolean exceededCapacity() {
        int count = 0;
        for (Node node : Jenkins.get().getNodes()) {
            if (node instanceof FirecrackerAgent) {
                count++;
            }
        }
        return count >= instanceCap;
    }
    
    @Override
    public boolean canProvision(Label label) {
        for (FirecrackerAgentTemplate template : templates) {
            if (label == null) {
                return true;
            }
            if (template.getLabelString() != null && !template.getLabelString().isEmpty()) {
                if (template.getLabel() != null && template.getLabel().equals(label)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private final class ProvisioningCallback implements Callable<Node> {
        private final FirecrackerAgentTemplate template;
        private final String nodeName;
        
        ProvisioningCallback(FirecrackerAgentTemplate template, String nodeName) {
            this.template = template;
            this.nodeName = nodeName;
        }
        
        @Override
        public Node call() throws Exception {
            FirecrackerVM vm = null;
            
            try {
                vm = new FirecrackerVM(
                        nodeName,
                        vmImagePath,
                        kernelImagePath,
                        memorySize,
                        vcpuCount,
                        networkInterface);
                
                vm.start();
                
                // Wait for VM to boot and get its IP
                String ipAddress = vm.getIpAddress();
                
                // Create and return the agent node
                return template.createNode(nodeName, ipAddress, vm);
            } catch (Exception e) {
                if (vm != null) {
                    vm.terminate();
                }
                throw e;
            }
        }
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {
        @Override
        public String getDisplayName() {
            return "Firecracker VM Cloud";
        }
        
        public FormValidation doCheckVmImagePath(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("VM image path is required");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckKernelImagePath(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("Kernel image path is required");
            }
            return FormValidation.ok();
        }
        
        public ListBoxModel doFillCredentialsIdItems() {
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            Jenkins.get(),
                            SSHUserPrivateKey.class,
                            Collections.emptyList(),
                            CredentialsMatchers.always());
        }
    }
}
