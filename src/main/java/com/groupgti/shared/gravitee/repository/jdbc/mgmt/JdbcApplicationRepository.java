/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcApplicationRepository extends JdbcAbstractCrudRepository<Application, String> implements ApplicationRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcApplicationRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Application.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Description", Types.NVARCHAR, String.class)
            .addColumn("Type", Types.NVARCHAR, String.class)
            .addColumn("Group", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("Status", Types.NVARCHAR, ApplicationStatus.class)
            .build(); 
    
    public JdbcApplicationRepository(DataSource dataSource) {
        super(dataSource, Application.class);
    }

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Application item) {
        return item.getId();
    }

    @Override
    public Set<Application> findByIds(List<String> ids) throws TechnicalException {
        logger.debug("JdbcApplicationRepository.findByIds({})", ids);

        try {
            List<Application> applications = jdbcTemplate.query("select * from Application where Id in ( " 
                    + ORM.buildInClause(ids) + " )"
                    , (PreparedStatement ps) -> { ORM.setArguments(ps, ids, 1); }
                    , ORM.getRowMapper()
            );
            return new HashSet<>(applications);
        } catch (Throwable ex) {
            logger.error("Failed to find applications by ids:", ex);
            throw new TechnicalException("Failed to find  applications by ids", ex);
        }
    }

    @Override
    public Set<Application> findAll(ApplicationStatus... ass) throws TechnicalException {
        logger.debug("JdbcApplicationRepository.findAll({})", (Object[])ass);

        try {
            List<ApplicationStatus> statuses = Arrays.asList(ass);
            
            StringBuilder query = new StringBuilder("select * from Application ");
            boolean first = true;
            ORM.buildInCondition(first, query, "Status", statuses);
            
            List<Application> applications = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> { ORM.setArguments(ps, statuses, 1); }
                    , ORM.getRowMapper()
            );
            logger.debug("Found {} applications: {}", applications.size(), applications);
            return new HashSet<>(applications);
        } catch (Throwable ex) {
            logger.error("Failed to find applications:", ex);
            throw new TechnicalException("Failed to find applications", ex);
        }
    }

    @Override
    public Set<Application> findByGroups(List<String> groupIds, ApplicationStatus... ass) throws TechnicalException {
        logger.debug("JdbcApplicationRepository.findByGroups({}, {})", groupIds, (Object)ass);

        try {
            List<ApplicationStatus> statuses = Arrays.asList(ass);
            
            StringBuilder query = new StringBuilder("select * from Application ");
            boolean first = true;
            first = ORM.buildInCondition(first, query, "Group", groupIds);
            ORM.buildInCondition(first, query, "Status", statuses);
                
            List<Application> applications = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = ORM.setArguments(ps, groupIds, 1); 
                        ORM.setArguments(ps, statuses, idx); 
                    }
                    , ORM.getRowMapper()
            );
            return new HashSet<>(applications);
        } catch (Throwable ex) {
            logger.error("Failed to find applications by groups:", ex);
            throw new TechnicalException("Failed to find applications by groups", ex);
        }
    }

    @Override
    public Set<Application> findByName(String partialName) throws TechnicalException {
        logger.debug("JdbcApplicationRepository.findByName({})", partialName);

        try {
            List<Application> applications = jdbcTemplate.query("select * from Application where Name like ?"
                    , ORM.getRowMapper()
                    , "%" + partialName + "%"
            );
            return new HashSet<>(applications);
        } catch (Throwable ex) {
            logger.error("Failed to find applications by name:", ex);
            throw new TechnicalException("Failed to find applications by name", ex);
        }
    }
    
    
    
}
