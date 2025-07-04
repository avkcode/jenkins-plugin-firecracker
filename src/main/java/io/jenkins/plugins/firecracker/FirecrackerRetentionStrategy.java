package io.jenkins.plugins.firecracker;

import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FirecrackerRetentionStrategy extends RetentionStrategy<FirecrackerAgent.FirecrackerComputer> {
    private static final Logger LOGGER = Logger.getLogger(FirecrackerRetentionStrategy.class.getName());
    
    private final String idleTerminationMinutes;
    
    public FirecrackerRetentionStrategy(String idleTerminationMinutes) {
        this.idleTerminationMinutes = idleTerminationMinutes;
    }
    
    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }
    
    @Override
    public long check(FirecrackerAgent.FirecrackerComputer computer) {
        if (computer.isIdle() && !computer.isOffline() && !computer.isConnecting() && computer.isAcceptingTasks()) {
            final long idleMilliseconds = System.currentTimeMillis() - computer.getIdleStartMilliseconds();
            
            try {
                int idleMinutes = Integer.parseInt(idleTerminationMinutes);
                if (idleMinutes > 0) {
                    final long idleTimeout = idleMinutes * 60 * 1000;
                    if (idleMilliseconds > idleTimeout) {
                        LOGGER.log(Level.INFO, "Disconnecting {0} after {1} idle minutes", 
                                new Object[]{computer.getName(), idleMinutes});
                        
                        computer.setTemporarilyOffline(true, new OfflineCause.IdleOfflineCause());
                        
                        Jenkins.get().getQueue().maintain();
                        
                        FirecrackerAgent node = computer.getNode();
                        if (node != null) {
                            LOGGER.log(Level.INFO, "Terminating {0} after idle timeout", computer.getName());
                            computer.setAcceptingTasks(false);
                            node.terminate();
                        }
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid idle termination minutes value: " + idleTerminationMinutes, e);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to terminate " + computer.getName(), e);
            }
        }
        
        return 1;
    }
    
    @Override
    public void start(FirecrackerAgent.FirecrackerComputer computer) {
        computer.connect(false);
    }
}
