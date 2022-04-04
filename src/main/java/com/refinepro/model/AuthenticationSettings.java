package com.refinepro.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationSettings {

    @JsonProperty("auth_endpoint")
    public final String auth_endpoint;
    @JsonProperty("auth_type_options")
    public final String[] auth_type_options;
    @JsonProperty("auth_type")
    public final String auth_type;
    @JsonProperty("auth_token_type_options")
    public final String[] auth_token_type_options;
    @JsonProperty("auth_token_type")
    public final String auth_token_type;
    @JsonProperty("auth_username_property")
    public final String auth_username_property;
    @JsonProperty("auth_password_property")
    public final String auth_password_property;
    @JsonProperty("auth_token_property")
    public final String auth_token_property;
    @JsonProperty("auth_header_options")
    public final String[] auth_header_options;
    @JsonProperty("auth_header")
    public final String auth_header;

    @JsonCreator
    public AuthenticationSettings(@JsonProperty("auth_endpoint") String auth_endpoint,
                                  @JsonProperty("auth_type_options") String[] auth_type_options,
                                  @JsonProperty("auth_type") String auth_type,
                                  @JsonProperty("auth_token_type_options") String[] auth_token_type_options,
                                  @JsonProperty("auth_token_type") String auth_token_type,
                                  @JsonProperty("auth_username_property") String auth_username_property,
                                  @JsonProperty("auth_password_property") String auth_password_property,
                                  @JsonProperty("auth_token_property") String auth_token_property,
                                  @JsonProperty("auth_header_options") String[] auth_header_options,
                                  @JsonProperty("auth_header") String auth_header) {
        this.auth_endpoint = auth_endpoint;
        this.auth_type_options = auth_type_options;
        this.auth_type = auth_type;
        this.auth_token_type_options = auth_token_type_options;
        this.auth_token_type = auth_token_type;
        this.auth_username_property = auth_username_property;
        this.auth_password_property = auth_password_property;
        this.auth_token_property = auth_token_property;
        this.auth_header_options = auth_header_options;
        this.auth_header = auth_header;
    }
}
