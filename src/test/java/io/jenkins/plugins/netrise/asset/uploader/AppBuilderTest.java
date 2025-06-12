package io.jenkins.plugins.netrise.asset.uploader;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.Secret;
import io.jenkins.plugins.netrise.asset.uploader.model.SubmitAssetInput;
import io.jenkins.plugins.netrise.asset.uploader.service.UploadService;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import hudson.model.Result;
import org.junit.*;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String name = "Bobby";
    final String artifact = "art.sh";

    final String model = "Model_1";
    final String version = "001";
    final String manufacturer = "Man_1";

    final String orgId = "Org_1";
    final String baseUrl = "http://test.org/test";
    final String clientId = "Client_ID_1";
    final String clientSecret = "Client_Secret_1";
    final String tokenUrl = "http://auth.test.org/test";
    final String audience = "Audit_1";

    @Mock
    private UploadService mockService;

    @Spy
    AppBuilder builder = new AppBuilder(artifact, name);

    private AutoCloseable closeable;

    @Before
    public void openMocks() {
        closeable = MockitoAnnotations. openMocks(this);
    }

    @After
    public void releaseMocks() throws Exception {
        closeable.close();
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new AppBuilder(artifact, name));
        project = jenkins.configRoundtrip(project);
        AppBuilder expected = new AppBuilder(artifact, name);
        expected.setModel("");
        expected.setVersion("");
        expected.setManufacturer("");
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripModel() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AppBuilder builder = new AppBuilder(artifact, name);
        builder.setModel(model);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        AppBuilder lhs = new AppBuilder(artifact, name);
        lhs.setModel(model);
        lhs.setVersion("");
        lhs.setManufacturer("");
        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripModel2() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AppBuilder builder = new AppBuilder(artifact, name);
        builder.setModel(model);
        builder.setVersion(version);
        builder.setManufacturer(manufacturer);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        AppBuilder lhs = new AppBuilder(artifact, name);
        lhs.setModel(model);
        lhs.setVersion(version);
        lhs.setManufacturer(manufacturer);
        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
    }

    @Test
    public void testEmptyNameAndArtifactFields() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new AppBuilder(artifact, ""));
        Assert.assertThrows(IllegalArgumentException.class, () -> new AppBuilder(artifact, null));
        Assert.assertThrows(IllegalArgumentException.class, () -> new AppBuilder("", name));
        Assert.assertThrows(IllegalArgumentException.class, () -> new AppBuilder(null, name));
    }

    @Test
    public void testNonExistingArtifact() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AppBuilder builder = new AppBuilder(artifact, name);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
        jenkins.assertLogContains("No such file in the workspace", build);
    }

    @Test
    public void testGlobalConfigValidity() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AppBuilder builder = new AppBuilder(artifact, name);
        project.getBuildersList().add(builder);

        FilePath ws = jenkins.jenkins.getWorkspaceFor(project);
        new FilePath(ws, artifact).write("Test data", "UTF-8");

        FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
        jenkins.assertLogContains("Parameter 'Organization ID' should be defined", build);
        jenkins.assertLogContains("Parameter 'Endpoint' should be defined", build);
        jenkins.assertLogContains("Parameter 'Client ID' should be defined", build);
        jenkins.assertLogContains("Parameter 'Client Secret' should be defined", build);
        jenkins.assertLogContains("Parameter 'Token URL' should be defined", build);
        jenkins.assertLogContains("Parameter 'Audience' should be defined", build);
    }

    @Test
    public void testBuildWithNotRealParameters() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AppBuilder builder = new AppBuilder(artifact, name);
        project.getBuildersList().add(builder);

        FilePath ws = jenkins.jenkins.getWorkspaceFor(project);
        Assert.assertNotNull(ws);
        new FilePath(ws, artifact).write("Test data", "UTF-8");

        builder.getDescriptor().setOrgId(orgId);
        builder.getDescriptor().setBaseUrl(baseUrl);
        builder.getDescriptor().setClientId(clientId);
        builder.getDescriptor().setClientSecret(Secret.fromString(clientSecret));
        builder.getDescriptor().setTokenUrl(tokenUrl);
        builder.getDescriptor().setAudience(audience);

        FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
        jenkins.assertLogContains("AuthException: Authentication error.", build);
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
//        AppBuilder builder = new AppBuilder(artifact, name);
        project.getBuildersList().add(builder);

        FilePath ws = jenkins.jenkins.getWorkspaceFor(project);
        Assert.assertNotNull(ws);
        new FilePath(ws, artifact).write("Test data", "UTF-8");

        builder.getDescriptor().setOrgId(orgId);
        builder.getDescriptor().setBaseUrl(baseUrl);
        builder.getDescriptor().setClientId(clientId);
        builder.getDescriptor().setClientSecret(Secret.fromString(clientSecret));
        builder.getDescriptor().setTokenUrl(tokenUrl);
        builder.getDescriptor().setAudience(audience);

        when(builder.getUploadService(builder.getDescriptor())).thenReturn(mockService);

        when(mockService.upload(any(Path.class), any(SubmitAssetInput.class)))
                .thenReturn("Uploaded_Asset_ID_1");

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        build.writeWholeLogTo(System.out);
        jenkins.assertLogContains("Uploaded_Asset_ID_1", build);
    }

    @Test
    public void testEnvVars() throws Exception {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars env = prop.getEnvVars();
        env.put("DEPLOY_TARGET", "staging");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        AppBuilder builder = new AppBuilder(artifact, name);
        builder.setModel("Model for ${DEPLOY_TARGET}");
        builder.setManufacturer("Man for ${DEPLOY_TARGET}");
        builder.setVersion("Version for ${DEPLOY_TARGET}");
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
        jenkins.assertLogContains("Model for staging", build);
        jenkins.assertLogContains("Man for staging", build);
        jenkins.assertLogContains("Version for staging", build);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = """
            node {
                writeFile text: 'hello', file: 'art.sh'
                uploadToNetRise artifact:'art.sh', name:'test_pipe'
            }
        """;

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
        completedBuild.writeWholeLogTo(System.out);
        String expectedString = "Parameter 'Organization ID' should be defined";
        jenkins.assertLogContains(expectedString, completedBuild);
    }
}
