package io.jenkins.plugins.netrise.asset.uploader;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;

import hudson.util.Secret;
import io.jenkins.plugins.netrise.asset.uploader.api.Client;
import io.jenkins.plugins.netrise.asset.uploader.env.EnvMapper;
import io.jenkins.plugins.netrise.asset.uploader.model.SubmitAssetInput;
import io.jenkins.plugins.netrise.asset.uploader.service.UploadService;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.verb.POST;

public class AppBuilder extends Builder implements SimpleBuildStep {

    public final static String URL_REGEX = "https?:\\/\\/(www\\.)?[\\w\\-]+\\.[a-z]{2,6}([\\/\\w\\.\\-\\?=%]*)?";

    private final String artifact;
    private final String name;
    private String model;
    private String version;
    private String manufacturer;

    @DataBoundConstructor
    public AppBuilder(String artifact, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Parameter 'name' should be defined and not empty.");
        }
        if (artifact == null || artifact.isBlank()) {
            throw new IllegalArgumentException("Parameter 'artifact' should be defined and not empty.");
        }
        this.artifact = artifact;
        this.name = name;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    @DataBoundSetter
    public void setModel(String value) {
        this.model = value;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    @DataBoundSetter
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    private String envy(String sentence, EnvVars env) {
        return sentence != null ? EnvMapper.replaceEnv(sentence, env) : null;
    }

    protected UploadService getUploadService(DescriptorImpl descriptor) {
        return new UploadService(
                URI.create(descriptor.getBaseUrl()),
                URI.create(descriptor.getTokenUrl()),
                descriptor.getOrgId(),
                descriptor.getClientId(),
                descriptor.getClientSecret().getPlainText(),
                descriptor.getAudience()
        );
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        listener.getLogger().println("\n\n=======================================================================\n");

        SubmitAssetInput input = new SubmitAssetInput(envy(name, env), envy(model, env), envy(version, env), envy(manufacturer, env));
        listener.getLogger().println("Asset: " + input);

        FilePath wsFile = workspace.child(artifact);
        listener.getLogger().println("File to upload: " + wsFile.toURI());

        if (!wsFile.exists()) {
            throw new RuntimeException("No such file in the workspace: " + wsFile);
        }

        DescriptorImpl descriptor = getDescriptor();

        // check global config
        descriptor.checkGlobalConfig();

        UploadService service = getUploadService(descriptor);

        // upload the artifact
        String assetId = service.upload(Path.of(wsFile.toURI()), input);

        listener.getLogger().println("Uploaded asset with id: " + assetId);

        // create detail page
        run.addAction(new SimpleAction(input.name(), assetId));
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Symbol("uploadToNetRise")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String orgId;
        private String baseUrl;
        private String tokenUrl;
        private String clientId;
        private Secret clientSecret;
        private String audience;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();
            return true;
        }

        private void checkGlobalFieldValidity(String name, Object value, List<String> errors) {
            if (value == null || value.toString().isBlank()) {
                errors.add( String.format("Parameter '%s' should be defined", name) );
            }
        }

        public void checkGlobalConfig() {
            List<String> errors = new ArrayList<>();
            checkGlobalFieldValidity("Organization ID", getOrgId(), errors);
            checkGlobalFieldValidity("Endpoint", getBaseUrl(), errors);
            checkGlobalFieldValidity("Client ID", getClientId(), errors);
            if (getClientSecret() == null || getClientSecret().getPlainText().isBlank()) {
                errors.add( String.format("Parameter '%s' should be defined", "Client Secret") );
            }
            checkGlobalFieldValidity("Token URL", getTokenUrl(), errors);
            checkGlobalFieldValidity("Audience", getAudience(), errors);

            if (!errors.isEmpty()) {
                throw new RuntimeException(
                        String.join("; ", errors)
                                + ". Configure the plugin there: Manage Jenkins -> Configure System -> "
                                + "NetRise");
            }
        }

        public String getOrgId() {
            return orgId;
        }

        public void setOrgId(String orgId) {
            this.orgId = orgId;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public Secret getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(Secret clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public FormValidation doCheckArtifact(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.isBlank()) {
                return FormValidation.error("Please set a path to the file");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.isBlank()) {
                return FormValidation.error("Please set a name");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckOrgId(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.isBlank()) {
                return FormValidation.error("Please set the Organization ID");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckBaseUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.isBlank()) {
                return FormValidation.error("Please set the Endpoint");
            }
            if (!value.matches(URL_REGEX)) {
                return FormValidation.warning("The URL should be valid");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckClientId(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.isBlank()) {
                return FormValidation.error("Please set the Client ID");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckClientSecret(@QueryParameter Secret value)
                throws IOException, ServletException {
            if (value.getPlainText().isBlank()) {
                return FormValidation.error("Please set the Client Secret");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckTokenUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.isBlank()) {
                return FormValidation.error("Please set the Token URL");
            }
            if (!value.matches(URL_REGEX)) {
                return FormValidation.warning("The URL should be valid");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckAudience(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.isBlank()) {
                return FormValidation.error("Please set the Audience");
            }

            return FormValidation.ok();
        }

        @POST
        public FormValidation doTestConnection(@QueryParameter("tokenUrl") final String tokenUrl,
                                               @QueryParameter("orgId") final String orgId,
                                               @QueryParameter("clientId") final String clientId,
                                               @QueryParameter("clientSecret") final String clientSecret,
                                               @QueryParameter("audience") final String audience,
                                               @AncestorInPath Job job) throws IOException, ServletException {
            try {
                if (job == null) {
                    Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                } else {
                    job.checkPermission(Item.CONFIGURE);
                }
                Client client = new Client(URI.create(tokenUrl), orgId, clientId, clientSecret, audience);
                client.authenticate();
                return FormValidation.ok("Authenticated successfully.");
            } catch (Exception e) {
                return FormValidation.error("Authentication error: " + e.getMessage());
            }
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "NetRise Plugin";
        }
    }
}
