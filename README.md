# AEM Multisite Link Externalizer

The AEM Multisite Externalizer is an enhanced version of the out-of-the-box AEM Link Externalizer component, designed to support multi-site and multi-tenant applications.

:information_source: Note: The AEM Multisite Externalizer (Open-Source Edition) is an open-source project and is not an officially supported Adobe product.

## Usage

The AEM Multisite Externalizer is intended to be used as a replacement for the out-of-the-box (OOTB) AEM externalizer. The configuration settings for this enhanced version follow the same format as the default externalizer, with the addition of a tenant name field.

:warning: Important: Many internal AEM components use the default Externalizer. It is crucial to maintain sensible default values in the OOTB Externalizer to ensure stable and predictable outcomes.

The tenant name you provide in the configuration is used to obtain a reference to the externalizer within your tenant's Java bundle. To do so, you'll need to change the way you reference the externalizer.

Instead of using:

    @Reference
    private Externalizer externalizer;

You should use an OSGi filter. This can be done by changing the above code to:

    @Reference(target="(tenantName=<your-site-name>)")
    private Externalizer externalizer;

In this snippet, <your-site-name> should be replaced with your specific tenant's site name.

Please note that there is no default setting available for the tenant name. If the OSGi filter doesn't find any matches for the provided tenant name, the reference will not be satisfied.

## Installation
### Download the Package

Download the zip file for the AEM Multisite Externalizer (Open-Source Edition) from the provided link in the repository.

### Open CRX Package Manager

Navigate to your instance's CRX Package Manager. This can typically be found at http://<your_AEM_instance>:<your_port>/crx/packmgr/index.jsp.

### Upload the Package

Click on the Upload Package button at the top left of the page. Browse for the downloaded zip file and click OK to upload it.

### Install the Package

Once the package is uploaded, you will see it in the list of packages in the CRX Package Manager. Locate the AEM Multisite Externalizer (Open-Source Edition) package and click on the Install button next to it.

### Verify Installation

After the installation is complete, ensure the package is installed correctly. You can do this by checking that the package status is shown as Installed.


## License

This project is licensed under the Apache License 2.0 - see the LICENSE.md file for details.
