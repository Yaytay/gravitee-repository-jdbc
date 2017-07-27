/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Date;
import java.util.HashSet;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 */
@Repository
public class JdbcUserRepository implements UserRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcUserRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(User.class, "Username")
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("Email", Types.NVARCHAR, String.class)
            .addColumn("Firstname", Types.NVARCHAR, String.class)
            .addColumn("LastConnectionAt", Types.TIMESTAMP, Date.class)
            .addColumn("Lastname", Types.NVARCHAR, String.class)
            .addColumn("Password", Types.NVARCHAR, String.class)
            .addColumn("Picture", Types.NVARCHAR, String.class)
            .addColumn("Source", Types.NVARCHAR, String.class)
            .addColumn("SourceId", Types.NVARCHAR, String.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("Username", Types.NVARCHAR, String.class)
            .build();

//    private static final ChildAdder<User> CHILD_ADDER = (User parent, ResultSet rs) -> {
//        Set<String> roles = parent.getRoles();
//        if (roles == null) {
//            roles = new HashSet<>();
//            parent.setRoles(roles);
//        }
//        if (rs.getString("Role") != null) {
//            roles.add(rs.getString("Role"));
//        }
//    };
    
    @Autowired
    public JdbcUserRepository(DataSource dataSource) {
        logger.debug("JdbcUserRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public User create(User user) throws TechnicalException {

        logger.debug("JdbcUserRepository.create({})", user);
        
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(user));
//            insertUserRoles(user);
            return findByUsername(user.getUsername()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create user:", ex);
            throw new TechnicalException("Failed to create user", ex);
        }

    }

//    protected void insertUserRoles(User user) throws DataAccessException {
//        List<String> filteredRoles = ORM.filterStrings(user.getRoles());
//        if (! filteredRoles.isEmpty()) {
//            jdbcTemplate.batchUpdate("insert into UserRole ( Username, Role ) values ( ? , ? )"
//                    , ORM.getBatchStringSetter(user.getUsername(), filteredRoles));
//        }
//    }

    @Override
    public User update(User user) throws TechnicalException {
        
        logger.debug("JdbcUserRepository.update({})", user);
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(user, user.getUsername()));
            jdbcTemplate.update("delete from UserRole where Username = ?", user.getUsername());
//            insertUserRoles(user);
            return findByUsername(user.getUsername()).get();
        } catch (Throwable ex) {
            logger.error("Failed to update user:", ex);
            throw new TechnicalException("Failed to update user", ex);
        }
        
    }


    @Override
    public Optional<User> findByUsername(String username) throws TechnicalException {

        logger.debug("JdbcUserRepository.findByUsername({})", username);
        try {
//            CollatingRowMapper<User> rowMapper = new CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Username");
            List<User> users = jdbcTemplate.query("select * from User u left join UserRole ur on u.Username = ur.Username where u.Username = ?"
                    , ORM.getRowMapper()
                    , username
            );
//            List<User> users = rowMapper.getRows();
            return users.stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find user by username:", ex);
            throw new TechnicalException("Failed to find user by username", ex);
        }

    }

    @Override
    public Set<User> findByUsernames(List<String> usernames) throws TechnicalException {
        
        logger.debug("JdbcUserRepository.findByUsernames({})", usernames);

        try {
//            CollatingRowMapper<User> rowMapper = new CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Username");
            List<User> users = jdbcTemplate.query("select * from User u left join UserRole ur on u.Username = ur.Username where u.Username in ( " 
                    + ORM.buildInClause(usernames) + " )"
                    , (PreparedStatement ps) -> { ORM.setArguments(ps, usernames, 1); }
                    , ORM.getRowMapper()
            );
//            List<User> users = rowMapper.getRows();
            return new HashSet<>(users);
        } catch (Throwable ex) {
            logger.error("Failed to find user by usernames:", ex);
            throw new TechnicalException("Failed to find user by usernames", ex);
        }
    }
    

    public Optional<User> findByEmail(String email) throws TechnicalException {

        logger.debug("JdbcUserRepository.findByEmail({})", email);
        try {
//            CollatingRowMapper<User> rowMapper = new CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Username");
            List<User> users = jdbcTemplate.query("select * from User u left join UserRole ur on u.Username = ur.Username where u.Email = ?"
                    , ORM.getRowMapper()
                    , email
            );
//            List<User> users = rowMapper.getRows();
            return users.stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find user by email:", ex);
            throw new TechnicalException("Failed to find user by email", ex);
        }

    }

    @Override
    public Set<User> findAll() throws TechnicalException {

        logger.debug("JdbcUserRepository.findAll()");
        try {
//            CollatingRowMapper<User> rowMapper = new CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Username");
            List<User> users = jdbcTemplate.query("select * from User u left join UserRole ur on u.Username = ur.Username"
                    , ORM.getRowMapper()
            );
//            List<User> users = rowMapper.getRows();
            return new HashSet<>(users);
        } catch (Throwable ex) {
            logger.error("Failed to find all users:", ex);
            throw new TechnicalException("Failed to find all users", ex);
        }
        
    }

    public Set<User> findByTeam(String teamName) throws TechnicalException {

        logger.debug("JdbcUserRepository.findByTeam({})", teamName);
        try {
//            CollatingRowMapper<User> rowMapper = new CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Username");
            List<User> users = jdbcTemplate.query("select * from User u "
                    + " left join UserRole ur on u.Username = ur.Username "
                    + " join TeamMember tm on u.Username = tm.Username "
                    + " where tm.TeamName = ? " 
                    , ORM.getRowMapper()
                    , teamName
            );
//            List<User> users = rowMapper.getRows();
            return new HashSet<>(users);
        } catch (Throwable ex) {
            logger.error("Failed to find user by team:", ex);
            throw new TechnicalException("Failed to find user by team", ex);
        }
    }
}
