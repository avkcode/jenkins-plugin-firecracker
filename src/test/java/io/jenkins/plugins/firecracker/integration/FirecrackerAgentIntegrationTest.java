package io.jenkins.plugins.firecracker.integration;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.slaves.NodeProperty;
import io.jenkins.plugins.firecracker.FirecrackerAgent;
import io.jenkins.plugins.firecracker.FirecrackerVM;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FirecrackerAgentIntegrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private FirecrackerVM mockVM;

    private FirecrackerAgent agent;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Set up the VM mock
        when(mockVM.getIpAddress()).thenReturn("192.168.122.100");
        
        // Create the agent
        agent = new FirecrackerAgent(
                "test-agent",
                "Test Agent",
                "/home/jenkins",
                1,
                new LabelAtom("test-label"),
                Collections.<NodeProperty<?>>emptyList(),
                "192.168.122.100",
                mockVM,
                "30");
        
        // Add agent to Jenkins
        jenkins.jenkins.addNode(agent);
    }

    @Test
    public void testAgentConfiguration() {
        // Verify agent properties
        assertEquals("test-agent", agent.getNodeName());
        assertEquals("Test Agent", agent.getNodeDescription());
        assertEquals("/home/jenkins", agent.getRemoteFS());
        assertEquals(1, agent.getNumExecutors());
        assertEquals("192.168.122.100", agent.getIpAddress());
        assertEquals("30", agent.getIdleTerminationMinutes());
        assertSame(mockVM, agent.getVm());
    }

    @Test
    public void testAgentComputer() {
        // Get the computer for the agent
        Computer computer = agent.toComputer();
        assertNotNull(computer);
        
        // Verify it's a FirecrackerComputer
        assertTrue(computer instanceof FirecrackerAgent.FirecrackerComputer);
        
        // Verify the computer's node is our agent
        assertSame(agent, computer.getNode());
    }

    @Test
    public void testTermination() throws Exception {
        // We can't directly call _terminate as it's protected
        // Instead, we can test the VM termination indirectly
        // For example, by removing the node from Jenkins
        jenkins.jenkins.removeNode(agent);
        
        // Verify the VM was terminated (this may need adjustment based on actual implementation)
        verify(mockVM, atLeastOnce()).terminate();
    }
}
