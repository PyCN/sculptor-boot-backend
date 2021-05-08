/*
 * Copyright © 2019-2021 CDK8S (cdk8s@qq.com)
 * All rights reserved.
 * 文件名称：RequestPermissionAspect.java
 * 项目名称：sculptor-boot-biz
 * 项目描述：sculptor-boot-biz
 * 版权说明：本软件属CDK8S所有
 */

package com.cdk8s.sculptor.aop.permission;


import com.cdk8s.sculptor.constant.GlobalConstant;
import com.cdk8s.sculptor.enums.DeleteEnum;
import com.cdk8s.sculptor.exception.ForbiddenException;
import com.cdk8s.sculptor.pojo.bo.service.bases.UserIdServiceBO;
import com.cdk8s.sculptor.service.SysPermissionService;
import com.cdk8s.sculptor.strategy.UserInfoContext;
import com.cdk8s.sculptor.util.CollectionUtil;
import com.cdk8s.sculptor.util.DatetimeUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(100)
@Aspect
@Component
@Slf4j
public class RequestPermissionAspect {

	@Autowired
	private SysPermissionService sysPermissionService;

	@SneakyThrows
	@Around("@annotation(requestPermission)")
	public Object around(ProceedingJoinPoint point, RequestPermission requestPermission) {
		Long currentUserId = UserInfoContext.getCurrentUserId();
		if (GlobalConstant.TOP_ADMIN_USER_ID.equals(currentUserId)) {
			return point.proceed();
		}

		Long currentUserTenantId = UserInfoContext.getCurrentUserTenantId();
		String strClassName = point.getTarget().getClass().getName();
		String strMethodName = point.getSignature().getName();
		log.debug("RequestPermissionAspect 类名:<{}>, 方法:<{}>", strClassName, strMethodName);
		long startTime = DatetimeUtil.currentEpochMilli();

		String[] permissionCode = requestPermission.value();
		int requestPermissionType = requestPermission.requestPermissionType();
		if (permissionCode.length > 0) {
			hasPermissionVerify(currentUserId, currentUserTenantId, permissionCode, requestPermissionType);
		}

		Object result = point.proceed();
		log.debug("RequestPermissionAspect 耗时:<{}ms>", (DatetimeUtil.currentEpochMilli() - startTime));
		return result;
	}

	private void hasPermissionVerify(Long currentUserId, Long currentUserTenantId, String[] permissionCode, int requestPermissionType) {
		UserIdServiceBO userIdServiceBO = new UserIdServiceBO();
		userIdServiceBO.setUserId(currentUserId);
		userIdServiceBO.setTenantId(currentUserTenantId);
		userIdServiceBO.setDeleteEnum(DeleteEnum.NOT_DELETED.getCode());

		List<String> permissionCodeList = sysPermissionService.findPermissionCodeListByUserId(userIdServiceBO);
		if (CollectionUtil.isEmpty(permissionCodeList)) {
			throw new ForbiddenException("您没有该操作的权限，请联系管理员处理");
		}

		List<String> list = CollectionUtil.toList(permissionCode);
		List<String> intersection = CollectionUtil.intersection(list, permissionCodeList);

		if (requestPermissionType == RequestPermissionEnum.OR_RELATION.getCode()) {
			if (intersection.size() < list.size()) {
				throw new ForbiddenException("您没有该操作的权限，请联系管理员处理");
			}
		}
		if (intersection.size() != list.size()) {
			throw new ForbiddenException("您没有该操作的权限，请联系管理员处理");
		}
	}


}
