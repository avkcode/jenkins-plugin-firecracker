package io.jenkins.plugins.firecracker.e2e;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.Shell;
import io.jenkins.plugins.firecracker.FirecrackerAgentTemplate;
import io.jenkins.plugins.firecracker.FirecrackerCloud;
import io.jenkins.plugins.firecracker.FirecrackerVM;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FirecrackerE2ETest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private FirecrackerVM mockVM;

    private FirecrackerCloud cloud;
    private FirecrackerAgentTemplate template;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Set up the VM mock with realistic behavior
        when(mockVM.getIpAddress()).thenReturn("192.168.122.100");
        
        // Mock the VM creation process
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Simulate VM startup time
                Thread.sleep(1000);
                return null;
            }
        }).when(mockVM).start();
        
        // Create a template
        template = new FirecrackerAgentTemplate();
        template.setLabelString("e2e-test");
        template.setDescription("E2E Test Agent");
        template.setNumExecutors(1);
        
        // Create the cloud
        cloud = new FirecrackerCloud("e2e-cloud");
        cloud.setVmImagePath("/path/to/rootfs.ext4");
        cloud.setKernelImagePath("/path/to/vmlinux");
        cloud.setCredentialsId("test-credentials");
        cloud.setTemplates(Collections.singletonList(template));
        
        // Add cloud to Jenkins
        jenkins.jenkins.clouds.add(cloud);
        
        // Mock the VM creation in the cloud
        // This requires modifying the test to work with the actual implementation
    }

    @Test
    public void testEndToEndJobExecution() throws Exception {
        // Create a freestyle project
        FreeStyleProject project = jenkins.createFreeStyleProject("e2e-test-job");
        
        // Add a simple shell build step
        project.getBuildersList().add(new Shell("echo 'Hello from Firecracker agent'"));
        
        // Assign the job to run on our label
        project.setAssignedLabel(Label.get("e2e-test"));
        
        // Schedule the build
        QueueTaskFuture<FreeStyleBuild> future = project.scheduleBuild2(0);
        
        // Wait for the build to complete (with timeout)
        FreeStyleBuild build = future.get(60, TimeUnit.SECONDS);
        
        // Verify the build was successful
        assertEquals(Result.SUCCESS, build.getResult());
        
        // Verify the console output contains our expected message
        String consoleOutput = jenkins.createWebClient().getPage(build, "console").asText();
        assertTrue("Build output should contain our echo message", 
                consoleOutput.contains("Hello from Firecracker agent"));
    }
}
