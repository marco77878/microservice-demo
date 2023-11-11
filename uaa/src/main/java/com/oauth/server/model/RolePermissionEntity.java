package com.oauth.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_role_permission")
@Builder
public class RolePermissionEntity implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)

    /**
     * 角色Id
     */
    private String roleId;

    /**
     *  权限Id
     */
    private String permissionId;


}
