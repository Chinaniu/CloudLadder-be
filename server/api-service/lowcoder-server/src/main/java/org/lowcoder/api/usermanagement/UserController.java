package org.lowcoder.api.usermanagement;

import static org.lowcoder.sdk.exception.BizError.INVALID_USER_STATUS;
import static org.lowcoder.sdk.util.ExceptionUtils.ofError;

import org.apache.commons.lang3.StringUtils;
import org.lowcoder.api.authentication.dto.OrganizationDomainCheckResult;
import org.lowcoder.api.framework.view.ResponseView;
import org.lowcoder.api.home.SessionUserService;
import org.lowcoder.api.home.UserHomeApiService;
import org.lowcoder.api.usermanagement.view.UpdateUserRequest;
import org.lowcoder.api.usermanagement.view.UserProfileView;
import org.lowcoder.domain.group.service.GroupMemberService;
import org.lowcoder.domain.group.service.GroupService;
import org.lowcoder.domain.organization.model.MemberRole;
import org.lowcoder.domain.organization.model.OrgMember;
import org.lowcoder.domain.organization.service.OrgMemberService;
import org.lowcoder.domain.user.constant.UserStatusType;
import org.lowcoder.domain.user.model.Connection;
import org.lowcoder.domain.user.model.User;
import org.lowcoder.domain.user.model.UserDetail;
import org.lowcoder.domain.user.model.UserStatus;
import org.lowcoder.domain.user.service.UserService;
import org.lowcoder.domain.user.service.UserStatusService;
import org.lowcoder.sdk.config.CommonConfig;
import org.lowcoder.sdk.exception.BizError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class UserController implements UserEndpoints
{

    @Autowired
    private SessionUserService sessionUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserHomeApiService userHomeApiService;

    @Autowired
    private OrgApiService orgApiService;

    @Autowired
    private UserStatusService userStatusService;

    @Autowired
    private UserApiService userApiService;

    @Autowired
    private CommonConfig commonConfig;

    @Autowired
    private OrgMemberService orgMemberService;

    @Autowired
    private GroupMemberService groupMemberService;

    @Autowired
    private GroupService groupService;

    @Override
    public Mono<ResponseView<?>> getUserProfile(ServerWebExchange exchange) {
        return sessionUserService.getVisitor()
                .flatMap(user -> userHomeApiService.buildUserProfileView(user, exchange))
                .flatMap(view -> orgApiService.checkOrganizationDomain()
                        .flatMap(OrganizationDomainCheckResult::buildOrganizationDomainCheckView)
                        .switchIfEmpty(Mono.just(ResponseView.success(view))));
    }

    @Override
    public Mono<ResponseView<Boolean>> newUserGuidanceShown() {
        return sessionUserService.getVisitorId()
                .flatMap(userHomeApiService::markNewUserGuidanceShown)
                .map(ResponseView::success);
    }

    @Override
    public Mono<ResponseView<Boolean>> markStatus(@RequestBody MarkUserStatusRequest request) {
        UserStatusType userStatusType = UserStatusType.fromValue(request.type());
        if (userStatusType == null) {
            return ofError(INVALID_USER_STATUS, "INVALID_USER_STATUS", request.type());
        }

        return sessionUserService.getVisitorId()
                .flatMap(visitorId -> userStatusService.mark(visitorId, userStatusType, request.value()))
                .map(ResponseView::success);
    }

    @Override
    public Mono<ResponseView<UserProfileView>> update(@RequestBody UpdateUserRequest updateUserRequest, ServerWebExchange exchange) {
        return sessionUserService.getVisitorId()
                .flatMap(uid -> {
                    User updateUser = new User();
                    if (StringUtils.isNotBlank(updateUserRequest.getName())) {
                        updateUser.setName(updateUserRequest.getName());
                        updateUser.setHasSetNickname(true);
                    }
                    return userService.update(uid, updateUser);
                })
                .flatMap(user -> userHomeApiService.buildUserProfileView(user, exchange))
                .map(ResponseView::success);
    }

    @Override
    public Mono<ResponseView<Boolean>> uploadProfilePhoto(@RequestPart("file") Mono<Part> fileMono) {
        return fileMono.zipWith(sessionUserService.getVisitor())
                .flatMap(tuple -> userService.saveProfilePhoto(tuple.getT1(), tuple.getT2()))
                .map(ResponseView::success);
    }

    @Override
    public Mono<ResponseView<Void>> deleteProfilePhoto() {
        return sessionUserService.getVisitor()
                .flatMap(visitor -> userService.deleteProfilePhoto(visitor)
                        .map(ResponseView::success));
    }

    @Override
    public Mono<Void> getProfilePhoto(ServerWebExchange exchange) {
        return sessionUserService.getVisitorId()
                .flatMap(userId -> getProfilePhoto(exchange, userId));
    }

    @Override
    public Mono<Void> getProfilePhoto(ServerWebExchange exchange, @PathVariable String userId) {
        return userService.getUserAvatar(exchange, userId)
                .switchIfEmpty(Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<ResponseView<Boolean>> updatePassword(@RequestBody UpdatePasswordRequest request) {
        if (StringUtils.isBlank(request.oldPassword()) || StringUtils.isBlank(request.newPassword())) {
            return ofError(BizError.INVALID_PARAMETER, "PASSWORD_EMPTY");
        }
        return sessionUserService.getVisitorId()
                .flatMap(user -> userService.updatePassword(user, request.oldPassword(), request.newPassword()))
                .map(ResponseView::success);
    }

    @Override
    public Mono<ResponseView<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (!commonConfig.isEnterpriseMode()) {
            return ofError(BizError.UNSUPPORTED_OPERATION, "BAD_REQUEST");
        }
        if (StringUtils.isBlank(request.userId())) {
            return ofError(BizError.INVALID_PARAMETER, "INVALID_USER_ID");
        }
        return userApiService.resetPassword(request.userId())
                .map(ResponseView::success);

    }

    @Override
    public Mono<ResponseView<Boolean>> setPassword(@RequestParam String password) {
        if (StringUtils.isBlank(password)) {
            return ofError(BizError.INVALID_PARAMETER, "PASSWORD_EMPTY");
        }
        return sessionUserService.getVisitorId()
                .flatMap(user -> userService.setPassword(user, password))
                .map(ResponseView::success);
    }

    @Override
    public Mono<ResponseView<UserDetail>> getCurrentUser(ServerWebExchange exchange) {
        return sessionUserService.getVisitor()
                .flatMap(user -> userService.buildUserDetail(user, false))
                .map(ResponseView::success);
    }

    @Override
    public Mono<ResponseView<?>> getUserDetail(@PathVariable("id") String userId) {
        return userApiService.getUserDetailById(userId)
                .map(ResponseView::success);
    }


    @Override
    public Mono<ResponseEntity<ResponseView<Map<String,Set<String>>>>> getFlowIseOperation(
            @PathVariable("createdBy") String createdBy,
            @PathVariable("orgId") String orgId
    ) {
        return orgMemberService.getCurrentOrgMember(createdBy)
                .flatMap(allOrgAdmins -> {

                    return  groupService.getAllUsersGroup(orgId)
                            .flatMap(group -> {
                                if (group.isAllUsersGroup()){
                                    return groupMemberService.getGroupMember(group.getId(), createdBy)
                                            .flatMap(groupMember -> {
                                                if (groupMember != null && allOrgAdmins.isAdmin()) {
                                                    return orgMemberService.getOrganizationMembers(orgId)
                                                            .map(OrgMember::getUserId)
                                                            .collect(Collectors.toSet())
                                                            .flatMap(adminIds ->{
                                                                return Flux.fromIterable(adminIds)
                                                                        .flatMap(adminId -> userService.findById(adminId))
                                                                        .filter(Objects::nonNull) // Filter out non-existing users
                                                                        .collect(Collectors.toSet())
                                                                        .flatMap(existingUsers -> {
                                                                            Set<String> existingAdminIds = existingUsers.stream()
                                                                                    .map(User::getId)
                                                                                    .collect(Collectors.toSet());

                                                                            Map<String,Set<String>> map = new HashMap<>();
                                                                            map.put(orgId,existingAdminIds);

                                                                            return Mono.just(ResponseEntity.ok().body(ResponseView.flowIseSuccess(ResponseView.SUCCESS, "IDs fetched successfully", map)));
                                                                        });
                                                            });
                                                } else if(allOrgAdmins.getRole() == MemberRole.MEMBER){
                                                    Map<String, Set<String>> result = new HashMap<>();
                                                    result.put(orgId, Collections.singleton(createdBy));
                                                    return Mono.just(ResponseEntity.ok().body(ResponseView.flowIseSuccess(ResponseView.SUCCESS, "Member member", result)));
                                                }
                                                Map<String, Set<String>> result = new HashMap<>();
                                                result.put(orgId, Collections.singleton(createdBy));
                                                return Mono.just(ResponseEntity.ok().body(ResponseView.flowIseSuccess(ResponseView.SUCCESS, "Dev Group", result)));
                                            });
                                }else if(group.isDevGroup()){
                                    Map<String, Set<String>> result = new HashMap<>();
                                    result.put(orgId, Collections.singleton(createdBy));
                                    return Mono.just(ResponseEntity.ok().body(ResponseView.flowIseSuccess(ResponseView.SUCCESS, "Dev Group", result)));
                                }
                                return Mono.just(ResponseEntity.ok().body(ResponseView.flowIseSuccess(ResponseView.SUCCESS, "No matching group found", Collections.emptyMap())));
                            });
                });

    }


}
