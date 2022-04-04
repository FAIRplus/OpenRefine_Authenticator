package com.refinepro.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Credentials {

    @JsonProperty("auth_username")
    public String auth_username;
    @JsonProperty("auth_password")
    public String auth_password;
    @JsonProperty("auth_token")
    public final String auth_token;

    @JsonCreator
    public Credentials(
            @JsonProperty("auth_username") String auth_username,
            @JsonProperty("auth_password") String auth_password,
            @JsonProperty("auth_token") String auth_token) {
        this.auth_username = auth_username;
        this.auth_password = auth_password;
        this.auth_token = auth_token;
    }
}
