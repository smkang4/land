package com.dage.rent.config;

import com.dage.rent.DTO.LoginDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final LoginDTO loginDTO;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(LoginDTO loginDTO) {
        this.loginDTO = loginDTO;
        this.authorities = new ArrayList<>();
        if (loginDTO.getDeptCode()=="0107") {
            this.authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            this.authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    public LoginDTO getLoginDTO() {
        return loginDTO;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return loginDTO.getUserPassword();
    }

    @Override
    public String getUsername() {
        return loginDTO.getUserId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public int getUserNo() {
        return loginDTO.getUserNo(); // LoginDTO에서 userNo를 리턴
    }
}