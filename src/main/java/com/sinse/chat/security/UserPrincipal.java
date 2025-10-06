
package com.sinse.chat.security;

import lombok.Getter;

import java.security.Principal;

@Getter
public class UserPrincipal implements Principal {
    private final String name; // This will be the userId as a String
    private final String nickname;
    private final String roleName;

    public UserPrincipal(String userId, String nickname, String roleName) {
        this.name = userId;
        this.nickname = nickname;
        this.roleName = roleName;
    }
}
