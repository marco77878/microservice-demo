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
@TableName("sys_user_role")
@Builder
public class UserRoleEntity implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    /**
     *  用户ID
     */
    private String userId;

    /**
     * 角色Id
     */
    private String roleId;


}
