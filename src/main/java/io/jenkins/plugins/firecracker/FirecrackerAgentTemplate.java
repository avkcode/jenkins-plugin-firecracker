package io.jenkins.plugins.firecracker;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.slaves.NodeProperty;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FirecrackerAgentTemplate implements Describable<FirecrackerAgentTemplate> {
    private String description;
    private String labelString;
    private int numExecutors = 1;
    private String remoteFS = "/home/jenkins";
    private int startupTimeoutSeconds = 180;
    private List<? extends NodeProperty<?>> nodeProperties = Collections.emptyList();
    private String idleTerminationMinutes = "30";
    private transient int templateId;
    
    @DataBoundConstructor
    public FirecrackerAgentTemplate() {
        this.templateId = (int) (System.currentTimeMillis() % 1000);
    }
    
    public String getDescription() {
        return description;
    }
    
    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLabelString() {
        return labelString;
    }
    
    @DataBoundSetter
    public void setLabelString(String labelString) {
        this.labelString = labelString;
    }
    
    public Label getLabel() {
        if (labelString == null || labelString.isEmpty()) {
            return null;
        }
        return Label.get(labelString);
    }
    
    public int getNumExecutors() {
        return numExecutors;
    }
    
    @DataBoundSetter
    public void setNumExecutors(int numExecutors) {
        this.numExecutors = numExecutors;
    }
    
    public String getRemoteFS() {
        return remoteFS;
    }
    
    @DataBoundSetter
    public void setRemoteFS(String remoteFS) {
        this.remoteFS = remoteFS;
    }
    
    public int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }
    
    @DataBoundSetter
    public void setStartupTimeoutSeconds(int startupTimeoutSeconds) {
        this.startupTimeoutSeconds = startupTimeoutSeconds;
    }
    
    public List<? extends NodeProperty<?>> getNodeProperties() {
        return Collections.unmodifiableList(nodeProperties);
    }
    
    @DataBoundSetter
    public void setNodeProperties(List<? extends NodeProperty<?>> nodeProperties) {
        this.nodeProperties = nodeProperties;
    }
    
    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }
    
    @DataBoundSetter
    public void setIdleTerminationMinutes(String idleTerminationMinutes) {
        this.idleTerminationMinutes = idleTerminationMinutes;
    }
    
    public String createNodeName() {
        return "firecracker-" + templateId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public FirecrackerAgent createNode(String nodeName, String ipAddress, FirecrackerVM vm) throws Descriptor.FormException, IOException {
        return new FirecrackerAgent(
                nodeName,
                description,
                remoteFS,
                numExecutors,
                labelString == null ? null : new LabelAtom(labelString),
                nodeProperties,
                ipAddress,
                vm,
                idleTerminationMinutes);
    }
    
    @Override
    public Descriptor<FirecrackerAgentTemplate> getDescriptor() {
        return Jenkins.get().getDescriptorByType(DescriptorImpl.class);
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<FirecrackerAgentTemplate> {
        @Override
        public String getDisplayName() {
            return "Firecracker Agent Template";
        }
    }
}
