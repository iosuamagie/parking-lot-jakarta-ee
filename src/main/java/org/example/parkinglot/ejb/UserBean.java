package org.example.parkinglot.ejb;

import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.example.parkinglot.common.Roles;
import org.example.parkinglot.common.UserDto;
import org.example.parkinglot.entities.User;
import org.example.parkinglot.entities.UserGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class UserBean {
    private static final Logger LOG = Logger.getLogger(UserBean.class.getName());

    @Inject
    PasswordBean passwordBean;

    @PersistenceContext
    EntityManager entityManager;

    public List<UserDto> findAllUsers() {
        LOG.info("findAllUsers");
        try {
            TypedQuery<User> typedQuery = entityManager.createQuery("SELECT u FROM User u", User.class);
            List<User> users = typedQuery.getResultList();
            return copyUsersToDto(users);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    private List<UserDto> copyUsersToDto(List<User> users) {
        List<UserDto> dtoList = new ArrayList<>();

        for (User user : users) {
            dtoList.add(new UserDto(user.getId(), user.getUsername(), user.getEmail()));
        }

        return dtoList;
    }

    public void createUser(String username, String email, String password, Collection<String> groups) {
        LOG.info("createUser");

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordBean.hash(password));

        entityManager.persist(newUser);

        assignGroupsToUser(username, groups);
    }

    // The login identity store reads the groups from the usergroups table, by username
    private void assignGroupsToUser(String username, Collection<String> groups) {
        for (String group : groups) {
            if (Roles.ALL.contains(group)) {
                entityManager.persist(new UserGroup(username, group));
            }
        }
    }

    private List<String> findGroupsByUsername(String username) {
        return entityManager
                .createQuery("SELECT g.userGroup FROM UserGroup g WHERE g.username = :username", String.class)
                .setParameter("username", username)
                .getResultList();
    }

    /**
     * @param exceptUserId the user being edited, or {@code null} for a new user
     */
    public boolean isUsernameTaken(String username, Long exceptUserId) {
        return countOthersWith("username", username, exceptUserId) > 0;
    }

    public boolean isEmailTaken(String email, Long exceptUserId) {
        return countOthersWith("email", email, exceptUserId) > 0;
    }

    private long countOthersWith(String field, String value, Long exceptUserId) {
        // field is one of our own constants, never user input
        return entityManager
                .createQuery("SELECT COUNT(u) FROM User u WHERE u." + field + " = :value AND u.id <> :exceptId", Long.class)
                .setParameter("value", value)
                .setParameter("exceptId", exceptUserId == null ? 0L : exceptUserId)
                .getSingleResult();
    }

    public Collection<String> findUsernamesByUserIds(Collection<Long> userIds) {
        return entityManager.createQuery("SELECT u.username FROM User u WHERE u.id IN :userIds", String.class)
                .setParameter("userIds", userIds)
                .getResultList();
    }

    /**
     * @return {@code false} if there is no user with this id
     */
    public boolean updateUser(Long userId, String username, String email, String password, Collection<String> groups) {
        LOG.info("updateUser");
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            return false;
        }

        // The groups are keyed by username, so they are rewritten under the (maybe new) username
        entityManager.createQuery("DELETE FROM UserGroup g WHERE g.username = :username")
                .setParameter("username", user.getUsername())
                .executeUpdate();

        user.setUsername(username);
        user.setEmail(email);

        // An empty password field means "keep the current password"
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordBean.hash(password));
        }

        assignGroupsToUser(username, groups);
        return true;
    }

    public UserDto findById(Long userId) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), findGroupsByUsername(user.getUsername()));
    }
}
