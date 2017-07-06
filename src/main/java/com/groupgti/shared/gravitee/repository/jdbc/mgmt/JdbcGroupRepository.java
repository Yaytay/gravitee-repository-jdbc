/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcGroupRepository implements GroupRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcGroupRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Group.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Type", Types.NVARCHAR, Group.Type.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build(); 
    
    private static final JdbcHelper.ChildAdder<Group> CHILD_ADDER = (Group parent, ResultSet rs) -> {
        if (parent.getAdministrators() == null) {
            parent.setAdministrators(new ArrayList<>());
        }
        if (rs.getString("Administrator") != null) {
            parent.getAdministrators().add(rs.getString("Administrator"));
        }
    };
    
    @Autowired
    public JdbcGroupRepository(DataSource dataSource) {
        logger.debug("JdbcGroupRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<Group> findById(String id) throws TechnicalException {

        logger.debug("JdbcGroupRepository.findById({})", id);
        
        try {
            JdbcHelper.CollatingRowMapper<Group> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from `Group` g left join `GroupAdministrator` ga on g.Id = ga.GroupId where Id = ?"
                    , rowMapper
                    , id
            );
            return rowMapper.getRows().stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find group by id:", ex);
            throw new TechnicalException("Failed to find group by id", ex);
        }
        
    }

    @Override
    public Group create(Group item) throws TechnicalException {
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            storeAdministrators(item, false);
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create api:", ex);
            throw new TechnicalException("Failed to create api", ex);
        }
    }

    @Override
    public Group update(Group item) throws TechnicalException {
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(item, item.getId()));
            storeAdministrators(item, true);
            return findById(item.getId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to update api:", ex);
            throw new TechnicalException("Failed to update api", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from GroupAdministrator where GroupId = ?", id);
        jdbcTemplate.update(ORM.getDeleteSql(), id);
    }
    
    private void storeAdministrators(Group group, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from GroupAdministrator where GroupId = ?", group.getId());
        }
        List<String> filteredAdministrators = ORM.filterStrings(group.getAdministrators());
        logger.debug("Storing administrators ({}) for {}", filteredAdministrators, group.getId());
        if (! filteredAdministrators.isEmpty()) {
            jdbcTemplate.batchUpdate("insert into GroupAdministrator ( GroupId, Administrator ) values ( ?, ? )"
                    , ORM.getBatchStringSetter(group.getId(), filteredAdministrators));
        }
        if (group.getAdministrators() == null) {
            group.setAdministrators(new ArrayList<>());
        }
    }

    @Override
    public Set<Group> findAll() throws TechnicalException {

        logger.debug("JdbcGroupRepository.findAll()");

        try {
            JdbcHelper.CollatingRowMapper<Group> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from `Group` g left join `GroupAdministrator` ga on g.Id = ga.GroupId "
                    , rowMapper
            );
            return new HashSet<>(rowMapper.getRows());
        } catch (Throwable ex) {
            logger.error("Failed to find all groups:", ex);
            throw new TechnicalException("Failed to find all groups", ex);
        }
        
    }

    @Override
    public Set<Group> findByType(Group.Type type) throws TechnicalException {

        logger.debug("JdbcGroupRepository.findByType({})", type);
        try {
            JdbcHelper.CollatingRowMapper<Group> rowMapper = new JdbcHelper.CollatingRowMapper<>(ORM.getRowMapper(), CHILD_ADDER, "Id");
            jdbcTemplate.query("select * from `Group` g left join `GroupAdministrator` ga on g.Id = ga.GroupId where g.Type = ?"
                    , rowMapper
                    , type.name()
            );
            List<Group> groups = rowMapper.getRows();
            return new HashSet<>(groups);
        } catch (Throwable ex) {
            logger.error("Failed to find groups by type:", ex);
            throw new TechnicalException("Failed to find groups by type", ex);
        }
    }
    
}
