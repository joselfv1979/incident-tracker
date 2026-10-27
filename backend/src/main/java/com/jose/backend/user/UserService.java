package com.jose.backend.user;

import com.jose.backend.common.PageResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public User getById(Long id) {
        return users.findById(id).orElseThrow(UserNotFoundException::new);
    }

    public PageResponse<UserResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        return PageResponse.from(users.findAll(pageable).map(UserResponse::from));
    }

    public List<UserSummary> assignable() {
        return users.findByRoleInOrderByNameAsc(List.of(Role.AGENT, Role.ADMIN)).stream()
                .map(UserSummary::from)
                .toList();
    }

    @Transactional
    public UserResponse changeRole(Long id, Role role, Long actorId) {
        if (id.equals(actorId)) {
            throw new OperationNotAllowedException("You cannot change your own role");
        }
        User user = getById(id);
        Role previous = user.getRole();
        user.setRole(role);
        log.info("Role of user {} changed from {} to {} by user {}", id, previous, role, actorId);
        return UserResponse.from(user);
    }
}
