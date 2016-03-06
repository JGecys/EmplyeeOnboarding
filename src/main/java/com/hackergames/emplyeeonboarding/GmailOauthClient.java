package com.hackergames.emplyeeonboarding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

@Configuration
@EnableOAuth2Client
public class GmailOauthClient {

    @Value("security.oauth2.client.client-id")
    String myClientId;

    @Bean
    @ConfigurationProperties("spring.oauth2.client")
    @Primary
    public ClientCredentialsResourceDetails oauth2RemoteResource() {
        ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
        details.setClientId(myClientId);
        return details;
    }

    @Bean
    public OAuth2ClientContext oauth2ClientContext() {
        return new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest());
    }

    @Bean
    @Primary
    public OAuth2RestTemplate oauth2RestTemplate(
            OAuth2ClientContext oauth2ClientContext,
            OAuth2ProtectedResourceDetails details) {
        return new OAuth2RestTemplate(details, oauth2ClientContext);
    }

}