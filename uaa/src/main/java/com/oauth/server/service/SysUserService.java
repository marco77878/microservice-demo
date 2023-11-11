package com.oauth.server.service;


import com.oauth.server.model.SysUserEntity;


public interface SysUserService {

    SysUserEntity selectByUsername(String username);

    boolean insert(SysUserEntity sysUserEntity);


}
