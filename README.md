# AEM Multisite Link Externalizer

This is a project template for AEM-based applications. It is intended as a best-practice set of examples as well as a potential starting point to develop your own functionality.

## Usage

The externalizer configuration can be found underneath the default AEM externalizer configuration. The configuration settings are the same format as the default externalizer, with the addition of a tenant name.

This tenant name is used to obtain a reference to the externalizer within your tenants Java bundle.

Instead of

    @Reference
    private Externalizer externalizer;

use an Osgi filter to acquire the reference to the externalizer

    @Reference(target="(tenantName=<your-site-name>)")
    private Externalizer externalizer;

Note that there is no default available. If the filter returns no results, the reference will be unsatisfied.

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with

    mvn clean install -PautoInstallPackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallPackagePublish

Or alternatively

    mvn clean install -PautoInstallPackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle
