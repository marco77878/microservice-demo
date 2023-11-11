package com.oauth.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oauth.server.mapper.SysUserMapper;
import com.oauth.server.model.SysUserEntity;
import com.oauth.server.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserEntity> implements SysUserService {


    @Override
    public SysUserEntity selectByUsername(String username) {
        LambdaQueryWrapper<SysUserEntity> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(SysUserEntity::getUsername,username);
        return this.getOne(lambdaQueryWrapper);
    }

    @Override
    public boolean insert(SysUserEntity sysUserEntity) {
      return save(sysUserEntity);
    }


}
