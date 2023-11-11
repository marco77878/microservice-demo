package com.oauth.server.authentication.ldap;

import com.oauth.server.constant.OAuth2Constant;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;


public class LdapGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public LdapGrantAuthenticationToken(Authentication clientPrincipal,
                                        @Nullable Map<String, Object> additionalParameters) {
        super(new AuthorizationGrantType(OAuth2Constant.GRANT_TYPE_LDAP),
                clientPrincipal, additionalParameters);
    }

}