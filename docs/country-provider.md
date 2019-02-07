## Country Provider

For country service clients you must provide a CountryProvider which specifies the country data you want. The `CountryProvider`
contains caching pre-population behaviour for the country data.

```java
@Provides
@Singleton
@Named("countryProviderExport")
CountryProvider provideCountryServiceExportClient(HttpExecutionContext httpContext, WSClient wsClient,
                                                  @Named("countryServiceAddress") String address,
                                                  @Named("countryServiceTimeout") int timeout,
                                                  @Named("countryServiceCredentials") String credentials) {
  CountryServiceClient client = CountryServiceClient.buildCountryServiceSetClient(address, timeout, credentials, wsClient, httpContext, "export-control");
  return new CountryProvider(client);
}
```
