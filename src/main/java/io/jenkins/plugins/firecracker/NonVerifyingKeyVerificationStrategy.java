package io.jenkins.plugins.firecracker;

import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ServerHostKeyVerifier;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.verifiers.HostKey;
import hudson.plugins.sshslaves.verifiers.SshHostKeyVerificationStrategy;
import hudson.slaves.SlaveComputer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Logger;

public class NonVerifyingKeyVerificationStrategy extends SshHostKeyVerificationStrategy {
    private static final Logger LOGGER = Logger.getLogger(NonVerifyingKeyVerificationStrategy.class.getName());
    
    @DataBoundConstructor
    public NonVerifyingKeyVerificationStrategy() {
    }
    
    @Override
    public boolean verify(SlaveComputer computer, HostKey hostKey, TaskListener listener) {
        // This strategy accepts any host key without verification
        // Note: This is not secure for production use, but simplifies development and testing
        return true;
    }
    
    public ServerHostKeyVerifier getVerifier() {
        return (hostname, port, serverHostKeyAlgorithm, serverHostKey) -> true;
    }
    
    public KnownHosts getKnownHosts() {
        return new KnownHosts();
    }
    
    @Extension
    public static class DescriptorImpl extends SshHostKeyVerificationStrategy.SshHostKeyVerificationStrategyDescriptor {
        @Override
        public String getDisplayName() {
            return "Non-verifying Strategy (Not secure, for testing only)";
        }
    }
}
