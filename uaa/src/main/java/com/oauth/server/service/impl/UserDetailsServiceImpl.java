package com.oauth.server.service.impl;


import com.oauth.server.model.SysUserEntity;
import com.oauth.server.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private SysUserService sysUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        SysUserEntity sysUserEntity = sysUserService.selectByUsername(username);
        if(Objects.isNull(sysUserEntity)){
            throw new UsernameNotFoundException("用户不存在！");
        }
        // TODO 关联数据库角色和权限
        List<SimpleGrantedAuthority> grantedAuthorityList = Stream.of("USER").map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new User(username,sysUserEntity.getPassword(),grantedAuthorityList);
    }
}
