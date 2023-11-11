/*
 * Copyright 2020-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oauth.server.federation;

// tag::imports[]
import java.util.function.Consumer;

import com.oauth.server.model.SysUserEntity;
import com.oauth.server.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
// end::imports[]

/**
 * Example {@link Consumer} to perform JIT provisioning of an {@link OAuth2User}.
 *
 * @author Steve Riesenberg
 * @since 1.1
 */
// tag::class[]


@Component
@Slf4j
public final class UserRepositoryOAuth2UserHandler implements Consumer<OAuth2User> {


	@Autowired
	private SysUserService sysUserService;

	@Override
	public void accept(OAuth2User user) {
		// Capture user in a local data store on first authentication
		if (this.sysUserService.selectByUsername(user.getName()) == null) {
			log.info("Saving first-time user: name=" + user.getName() + ", claims=" + user.getAttributes() + ", authorities=" + user.getAuthorities());
			SysUserEntity userEntity = SysUserEntity.builder().username(user.getName()).build();
			// TODO 关联用户角色和权限
			this.sysUserService.insert(userEntity);
		}
	}

}
// end::class[]
