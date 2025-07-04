package io.jenkins.plugins.firecracker.integration;

import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.NodeProvisioner;
import io.jenkins.plugins.firecracker.FirecrackerAgent;
import io.jenkins.plugins.firecracker.FirecrackerAgentTemplate;
import io.jenkins.plugins.firecracker.FirecrackerCloud;
import io.jenkins.plugins.firecracker.FirecrackerVM;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FirecrackerCloudIntegrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private FirecrackerVM mockVM;

    private FirecrackerCloud cloud;
    private FirecrackerAgentTemplate template;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Set up the VM mock
        when(mockVM.getIpAddress()).thenReturn("192.168.122.100");
        
        // Create a template
        template = new FirecrackerAgentTemplate();
        template.setLabelString("test-label");
        template.setDescription("Test Agent");
        template.setNumExecutors(1);
        
        // Create the cloud
        cloud = new FirecrackerCloud("test-cloud");
        cloud.setVmImagePath("/path/to/rootfs.ext4");
        cloud.setKernelImagePath("/path/to/vmlinux");
        cloud.setCredentialsId("test-credentials");
        cloud.setTemplates(Collections.singletonList(template));
        
        // Add cloud to Jenkins
        jenkins.jenkins.clouds.add(cloud);
    }

    @Test
    public void testCanProvision() {
        // Test with matching label
        Label label = Label.get("test-label");
        assertTrue(cloud.canProvision(label));
        
        // Test with non-matching label
        Label nonMatchingLabel = Label.get("other-label");
        assertFalse(cloud.canProvision(nonMatchingLabel));
        
        // Test with null label
        assertTrue(cloud.canProvision(null));
    }

    @Test
    public void testProvision() {
        Label label = Label.get("test-label");
        Collection<NodeProvisioner.PlannedNode> plannedNodes = cloud.provision(label, 1);
        
        // Verify we got one planned node
        assertEquals(1, plannedNodes.size());
        
        // Verify the planned node has the correct number of executors
        NodeProvisioner.PlannedNode plannedNode = plannedNodes.iterator().next();
        assertEquals(1, plannedNode.numExecutors);
    }
}
