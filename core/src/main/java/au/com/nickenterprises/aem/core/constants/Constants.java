package au.com.nickenterprises.aem.core.constants;

public final class Constants {
    public static final String[] DEFAULT_DOMAINS = new String[]{"local http://localhost:4502", "author http://localhost:4502", "publish http://localhost:4503"};

    public static final String TENANT_FIELD_NAME = "Tenant name";
    public static final String DOMAINS_FIELD_NAME = "Domains";
    public static final String ENCODED_FIELD_NAME = "Assume encoded path";

    public static final String TENANT_FIELD_DESC = "Name of this tenant. This name must match the value passed to the Osgi filter used to acquire the Externalizer service.";
    public static final String DOMAINS_FIELD_DESC = "List of domain mappings. In the form: \"name [scheme://]domain.com[:port][/contextpath]\". Standard required names are \"publish\" (public website DNS, such as \"http://www.mysite.com\"), \"author\" (author DNS, such as \"https://author.mysite.com\") and \"local\" (this instance directly). The scheme will be used as default scheme (if not specified by the code) and can globally define whether http or https is desired. The context path must match the installation of the sling launchpad webapp on that instance. Additional custom domains can be added, each with a unique name.";
    public static final String ENCODED_FIELD_DESC = "If active, Externalizer assumes that resource paths passed to its methods are URL-encoded. This might be necessary if your resource paths potentially contain \"?\" and/or \"#\" (which would be considered to start the query string/the fragment of the URL otherwise). Note that activating this setting may cause issues with other parts of the application that assume non-encoded paths if affected resource paths contain characters that need to be URL-encoded.";

    public static final int HTTP_PORT = 80;
    public static final int HTTPS_PORT = 443;
    
    private Constants() {
        // Empty constructor. No constructor.
    }
}
