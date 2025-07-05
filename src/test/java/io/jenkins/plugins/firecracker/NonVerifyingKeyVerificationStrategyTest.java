package io.jenkins.plugins.firecracker;

import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.verifiers.HostKey;
import hudson.slaves.SlaveComputer;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class NonVerifyingKeyVerificationStrategyTest extends hudson.model.AbstractDescribableImplTest {

    public NonVerifyingKeyVerificationStrategyTest() {
        super(NonVerifyingKeyVerificationStrategy.class);
    }

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    @Mock
    private SlaveComputer mockComputer;
    
    private HostKey realHostKey;
    
    @Mock
    private TaskListener mockListener;

    private NonVerifyingKeyVerificationStrategy strategy;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new NonVerifyingKeyVerificationStrategy();
        // Create a real HostKey instance instead of mocking
        realHostKey = new HostKey("localhost", new byte[]{1,2,3});
    }

    @Test
    public void testVerifyAlwaysReturnsTrue() {
        // The strategy should always return true regardless of input
        assertTrue(strategy.verify(mockComputer, realHostKey, mockListener));
        
        // Test with null values to ensure it's robust
        assertTrue(strategy.verify(null, realHostKey, mockListener));
        assertTrue(strategy.verify(mockComputer, null, mockListener));
        assertTrue(strategy.verify(mockComputer, realHostKey, null));
    }

    @Test
    public void testGetVerifier() {
        // Get the verifier
        com.trilead.ssh2.ServerHostKeyVerifier verifier = strategy.getVerifier();
        assertNotNull(verifier);
        
        try {
            // The verifier should always return true
            assertTrue(verifier.verifyServerHostKey("localhost", 22, "ssh-rsa", new byte[]{1, 2, 3}));
        } catch (Exception e) {
            fail("Verifier threw an exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetKnownHosts() {
        // Get the known hosts
        com.trilead.ssh2.KnownHosts knownHosts = strategy.getKnownHosts();
        assertNotNull(knownHosts);
    }

    @Test
    public void testDescriptor() {
        // Get the descriptor through Jenkins instance
        Descriptor<?> descriptor = jenkins.getInstance().getDescriptorOrDie(NonVerifyingKeyVerificationStrategy.class);
        
        // Verify the display name
        assertEquals("Non-verifying Strategy (Not secure, for testing only)", 
                descriptor.getDisplayName());
    }
}
