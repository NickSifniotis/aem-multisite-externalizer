/*
 *  Copyright 2023 Nick Enterprises Pty Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package au.com.nickenterprises.aem.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.text.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static au.com.nickenterprises.aem.core.constants.Constants.*;

@Component(
        service = Externalizer.class,
        property = {
                "service.description=Day CQ Externalizer - Multi-Site Support",
                "service.ranking:Integer=-1",
                "service.scope=bundle"
        },
        immediate = true
)
@Designate(ocd = MultiSiteExternalizerImpl.MultiTenantExternalizerConfig.class, factory = true)
public class MultiSiteExternalizerImpl implements Externalizer {
    @ObjectClassDefinition(
            name = "Day CQ Link Externalizer - Multi Site",
            description = "Creates absolute URLs"
    )
    public @interface MultiTenantExternalizerConfig {
        @AttributeDefinition(name = TENANT_FIELD_NAME, description = TENANT_FIELD_DESC)
        String tenantName();

        @AttributeDefinition(name = DOMAINS_FIELD_NAME, description = DOMAINS_FIELD_DESC)
        String[] domains() default {"local http://localhost:4502", "author http://localhost:4502", "publish http://localhost:4503"};

        @AttributeDefinition(name = ENCODED_FIELD_NAME, description = ENCODED_FIELD_DESC)
        boolean encodedPath() default false;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MultiSiteExternalizerImpl.class);

    private Map<String, URI> domains;
    private boolean assumeEncodedPath;
    private String tenantName;

    public MultiSiteExternalizerImpl() {
        // Empty constructor
    }

    @Activate
    @Modified
    protected void activate(MultiTenantExternalizerConfig config) {
        this.tenantName = config.tenantName();
        this.assumeEncodedPath = config.encodedPath();
        parseDomainConfig(config.domains());

        LOG.debug("Registering externalizer for tenant {}", this.tenantName);
    }

    private void parseDomainConfig(String[] domainCfgs) {
        this.domains = new HashMap<>();
        if (domainCfgs == null) {
            return;
        }

        for (String domainCfg: domainCfgs) {
            domainCfg = domainCfg.trim();
            int splitAt = domainCfg.indexOf(' ');
            if (splitAt > 0) {
                String name = domainCfg.substring(0, splitAt);
                String domain = domainCfg.substring(splitAt + 1);
                if (!domain.contains("://")) {
                    domain = "http://" + domain;
                }

                this.domains.put(name, URI.create(domain));
            }
        }
    }

    private static String getAuthority(String scheme, String host, int port) {
        String result = host;

        if (port <= 0) {
            return result;
        }

        if ((!"http".equals(scheme) || port != HTTP_PORT) && (!"https".equals(scheme) || port != HTTPS_PORT)) {
            result += ":" + port;
        }

        return result;
    }

    private static String encodePath(String path) {
        StringBuilder encoded = new StringBuilder(path.length());
        String[] pathSegs = path.split("/");

        for(String seg: pathSegs) {
            if (seg.length() > 0) {
                encoded.append("/");

                try {
                    encoded.append(URLEncoder.encode(seg, "UTF-8").replaceAll("\\+", "%20"));
                } catch (UnsupportedEncodingException e) {
                    LOG.warn("Unsupported encoding; appending original segment: {}", seg);
                    LOG.warn(String.valueOf(e));
                    encoded.append(seg);
                }
            }
        }

        return encoded.toString();
    }

    private String determineActualPath(ResourceResolver resolver, String path) {
        if (path == null) {
            return null;
        }

        String resolverPath = path;
        String suffix = "";
        int sepPos = path.indexOf("?");
        if (sepPos >= 0) {
            resolverPath = path.substring(0, sepPos);
            suffix = path.substring(sepPos);
        } else {
            sepPos = path.indexOf("#");
            if (sepPos >= 0) {
                resolverPath = path.substring(0, sepPos);
                suffix = path.substring(sepPos);
            }
        }

        if (resolver != null) {
            if (this.assumeEncodedPath) {
                try {
                    resolverPath = URLDecoder.decode(resolverPath, "UTF-8");
                    resolverPath = Text.replace(resolverPath, "?", "\u0001");
                    resolverPath = Text.replace(resolverPath, "#", "\u0002");
                } catch (UnsupportedEncodingException e) {
                    LOG.warn("UnsupportedEncodingException: {}", e.getMessage());
                }
            }

            path = resolver.map(resolverPath) + suffix;
            if (this.assumeEncodedPath) {
                path = Text.replace(path, "%01", "%3F");
                path = Text.replace(path, "%02", "%23");
            }
        } else if (!this.assumeEncodedPath) {
            path = encodePath(resolverPath) + suffix;
        }

        return path;
    }

    public String relativeLink(SlingHttpServletRequest request, String path) {
        return request.getResourceResolver().map(request, path);
    }

    public String absoluteLink(SlingHttpServletRequest request, String scheme, String path) {
        return this.absoluteLink(request, null, scheme, path);
    }

    private String absoluteLink(SlingHttpServletRequest request, ResourceResolver resolver, String scheme, String path) {
        if (request == null) {
            return this.externalLink(resolver, "local", scheme, path);
        } else {
            StringBuilder url = new StringBuilder();
            url.append(scheme).append("://");
            if (resolver == null) {
                resolver = request.getResourceResolver();
            }

            URI uri = URI.create(this.determineActualPath(resolver, path));
            if (uri.getRawAuthority() == null) {
                url.append(getAuthority(scheme, request.getServerName(), request.getServerPort()));
            } else {
                url.append(uri.getRawAuthority());
            }

            url.append(request.getContextPath());
            url.append(uri.getRawPath());
            if (uri.getRawQuery() != null) {
                url.append("?");
                url.append(uri.getRawQuery());
            }

            if (uri.getRawFragment() != null) {
                url.append("#");
                url.append(uri.getRawFragment());
            }

            LOG.debug("externalizing absolute link (request scope): {} -> {}", path, url);
            return url.toString();
        }
    }

    /** @deprecated */
    @Deprecated
    public String absoluteLink(ResourceResolver resolver, String scheme, String path) {
        return this.externalLink(resolver, LOCAL, scheme, path);
    }

    /** @deprecated */
    @Deprecated
    public String absoluteLink(String scheme, String path) {
        return this.externalLink(null, LOCAL, scheme, path);
    }

    public String externalLink(ResourceResolver resolver, String domain, String scheme, String path) {
        LOG.debug("Resolving path {} for tenant {}", path, this.tenantName);

        if (domain == null) {
            throw new IllegalArgumentException("Argument 'domain' is null");
        }
        URI domainURI = this.domains.get(domain);
        if (domainURI == null) {
            throw new IllegalArgumentException("Could not find configuration for domain '" + domain + "'");
        }

        StringBuilder url = new StringBuilder();
        if (scheme == null) {
            scheme = domainURI.getScheme();
            if (scheme == null) {
                scheme = "http";
            }
        }

        url.append(scheme).append("://");
        URI mapped = URI.create(this.determineActualPath(resolver, path));
        if (mapped.getRawAuthority() == null) {
            url.append(getAuthority(scheme, domainURI.getHost(), domainURI.getPort()));
        } else {
            url.append(mapped.getRawAuthority());
        }

        if (domainURI.getRawPath() != null) {
            url.append(domainURI.getRawPath());
        }

        url.append(mapped.getRawPath());
        if (mapped.getRawQuery() != null) {
            url.append("?");
            url.append(mapped.getRawQuery());
        }

        if (mapped.getRawFragment() != null) {
            url.append("#");
            url.append(mapped.getRawFragment());
        }

        LOG.debug("externalizing link for '{}': {} -> {}", domain, path, url);
        return url.toString();
    }

    public String publishLink(ResourceResolver resolver, String path) {
        return this.externalLink(resolver, PUBLISH, null, path);
    }

    public String publishLink(ResourceResolver resolver, String scheme, String path) {
        return this.externalLink(resolver, PUBLISH, scheme, path);
    }

    public String authorLink(ResourceResolver resolver, String path) {
        return this.externalLink(resolver, AUTHOR, null, path);
    }

    public String authorLink(ResourceResolver resolver, String scheme, String path) {
        return this.externalLink(resolver, AUTHOR, scheme, path);
    }

    public String externalLink(ResourceResolver resolver, String domain, String path) {
        return this.externalLink(resolver, domain, null, path);
    }

}
