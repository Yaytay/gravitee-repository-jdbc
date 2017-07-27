/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import java.sql.Types;
import java.util.Date;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import java.sql.PreparedStatement;
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
public class JdbcMembershipRepository implements MembershipRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcMembershipRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Membership.class, "Key")
            .updateSql("update Membership set "
                    + " `UserId` = ?"
                    + " , `ReferenceType` = ?"
                    + " , `ReferenceId` = ?"
                    + " , `RoleScope` = ?"
                    + " , `RoleName` = ?"
                    + " , `CreatedAt` = ? "
                    + " , `UpdatedAt` = ? "
                    + " where "
                    + " UserId = ? "
                    + " and ReferenceType = ? "
                    + " and ReferenceId = ? "
            )
            .addColumn("UserId", Types.NVARCHAR, String.class)
            .addColumn("ReferenceType", Types.NVARCHAR, MembershipReferenceType.class)
            .addColumn("ReferenceId", Types.NVARCHAR, String.class)
            .addColumn("RoleScope", Types.INTEGER, int.class)
            .addColumn("RoleName", Types.NVARCHAR, String.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build();    
    
    @Autowired
    public JdbcMembershipRepository(DataSource dataSource) {
        logger.debug("JdbcMembershipRepository({})", dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Membership create(Membership membership) throws TechnicalException {
        
        logger.debug("JdbcMembershipRepository.create({})", membership);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(membership));
            return findById(membership.getUserId(), membership.getReferenceType(), membership.getReferenceId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to create membership:", ex);
            throw new TechnicalException("Failed to create membership", ex);
        }
        
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        
        logger.debug("JdbcMembershipRepository.update({})", membership);
        try {
            jdbcTemplate.update(ORM.buildUpdatePreparedStatementCreator(membership
                    , membership.getUserId()
                    , membership.getReferenceType().name()
                    , membership.getReferenceId()
            ));
            return findById(membership.getUserId(), membership.getReferenceType(), membership.getReferenceId()).get();
        } catch (Throwable ex) {
            logger.error("Failed to update membership:", ex);
            throw new TechnicalException("Failed to update membership", ex);
        }
        
    }

    @Override
    public void delete(Membership membership) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.delete({})", membership);
        try {
            jdbcTemplate.update("delete from Membership where UserId = ? and ReferenceType = ? and ReferenceId = ? "
                    , membership.getUserId()
                    , membership.getReferenceType().name()
                    , membership.getReferenceId()
            );
        } catch (Throwable ex) {
            logger.error("Failed to create item:", ex);
            throw new TechnicalException("Failed to create item", ex);
        }
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {

        logger.debug("JdbcMembershipRepository.findById({}, {}, {})", userId, referenceType, referenceId);
        
        try {
            List<Membership> items = jdbcTemplate.query("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `RoleScope`, `RoleName`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership where UserId = ? and ReferenceType = ? and ReferenceId = ?"
                    , ORM.getRowMapper()
                    , userId
                    , referenceType.name()
                    , referenceId
            );
            return items.stream().findFirst();
        } catch (Throwable ex) {
            logger.error("Failed to find membership by id:", ex);
            throw new TechnicalException("Failed to find membership by id", ex);
        }
        
    }

    @Override
    public Set<Membership> findByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.findByReferenceAndRole({}, {}, {}, {})", referenceType, referenceId, roleScope, roleName);
        
        try {
            StringBuilder query = new StringBuilder("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `RoleScope`, `RoleName`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership ");
            boolean first = true;
            if (referenceType != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" ReferenceType = ? ");                
            }
            if (referenceId != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" ReferenceId = ? ");                
            }
            if (roleScope != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleScope = ? ");                
            }
            if (roleName != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleName = ? ");                
            }            
            List<Membership> items = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = 1;
                        if (referenceType != null) {
                            ps.setString(idx++, referenceType.name());
                        }
                        if (referenceId != null) {
                            ps.setString(idx++, referenceId);
                        }
                        if (roleScope != null) {
                            ps.setInt(idx++, roleScope.getId());
                        }
                        if (roleName != null) {
                            ps.setString(idx++, roleName);
                        }
                    }, ORM.getRowMapper()
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by references and membership type:", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
        
    }

    @Override
    public Set<Membership> findByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.findByReferencesAndRole({}, {}, {}, {})", referenceType, referenceIds, roleScope, roleName);
        
        try {
            StringBuilder query = new StringBuilder("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `RoleScope`, `RoleName`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership ");
            boolean first = true;
            if (referenceType != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" ReferenceType = ? ");                
            }
            ORM.buildInCondition(first, query, "ReferenceId", referenceIds);
            if (roleScope != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleScope = ? ");                
            }
            if (roleName != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleName = ? ");                
            }            
            List<Membership> items = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = 1;
                        if (referenceType != null) {
                            ps.setString(idx++, referenceType.name());
                        }
                        idx = ORM.setArguments(ps, referenceIds, idx);
                        if (roleScope != null) {
                            ps.setInt(idx++, roleScope.getId());
                        }
                        if (roleName != null) {
                            ps.setString(idx++, roleName);
                        }
                    }, ORM.getRowMapper()
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by references and membership type:", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
        
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndRole(String userId, MembershipReferenceType referenceType, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("JdbcMembershipRepository.findByUserAndReferenceTypeAndRole({}, {}, {}, {})", userId, referenceType, roleScope, roleName);
        
        try {
            StringBuilder query = new StringBuilder("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `RoleScope`, `RoleName`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership ");
            boolean first = true;
            if (userId != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" UserId = ? ");                
            }
            if (referenceType != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" ReferenceType = ? ");                
            }
            if (roleScope != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleScope = ? ");                
            }
            if (roleName != null) {
                query.append(first ? " where " : " and ");
                first = false;
                query.append(" RoleName = ? ");                
            }            
            List<Membership> items = jdbcTemplate.query(query.toString()
                    , (PreparedStatement ps) -> {
                        int idx = 1;
                        if (userId != null) {
                            ps.setString(idx++, userId);
                        }
                        if (referenceType != null) {
                            ps.setString(idx++, referenceType.name());
                        }
                        if (roleScope != null) {
                            ps.setInt(idx++, roleScope.getId());
                        }
                        if (roleName != null) {
                            ps.setString(idx++, roleName);
                        }
                    }, ORM.getRowMapper()
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by references and membership type:", ex);
            throw new TechnicalException("Failed to find membership by references and membership type", ex);
        }
        
    }

//    
//    @Override
//    public Set<Membership> findByReferenceAndMembershipType(MembershipReferenceType referenceType, String referenceId, String membershipType) throws TechnicalException {
//
//        logger.debug("JdbcMembershipRepository.findByReferenceAndMembershipType({}, {}, {})", referenceType, referenceId, membershipType);
//        
//        return findByReferencesAndMembershipType(referenceType, Arrays.asList(referenceId), membershipType);
//        
//    }
//
//    @Override
//    public Set<Membership> findByReferencesAndMembershipType(MembershipReferenceType referenceType, List<String> referenceIds, String membershipType) throws TechnicalException {
//
//        logger.debug("JdbcMembershipRepository.findByReferencesAndMembershipType({}, {}, {})", referenceType, referenceIds, membershipType);
//        
//        try {
//            StringBuilder query = new StringBuilder("select * from Membership ");
//            boolean first = true;
//            if (referenceType != null) {
//                query.append(first ? " where " : " and ");
//                first = false;
//                query.append(" ReferenceType = ? ");                
//            }
//            if (membershipType != null) {
//                query.append(first ? " where " : " and ");
//                first = false;
//                query.append(" Type = ? ");                
//            }
//            ORM.buildInCondition(first, query, "ReferenceId", referenceIds);
//            
//            List<Membership> items = jdbcTemplate.query(query.toString()
//                    , (PreparedStatement ps) -> {
//                        int idx = 1;
//                        if (referenceType != null) {
//                            ps.setString(idx++, referenceType.name());
//                        }
//                        if (membershipType != null) {
//                            ps.setString(idx++, membershipType);
//                        }
//                        ORM.setArguments(ps, referenceIds, idx);
//                    }, ORM.getRowMapper()
//            );
//            return new HashSet<>(items);
//        } catch (Throwable ex) {
//            logger.error("Failed to find membership by references and membership type:", ex);
//            throw new TechnicalException("Failed to find membership by references and membership type", ex);
//        }
//        
//    }
//
//    @Override
//    public Set<Membership> findByUserAndReferenceTypeAndMembershipType(String userId, MembershipReferenceType referenceType, String membershipType) throws TechnicalException {
//
//        logger.debug("JdbcMembershipRepository.findByUserAndReferenceTypeAndMembershipType({}, {}, {})", userId, referenceType);
//        
//        try {
//            List<Membership> items = jdbcTemplate.query("select * from Membership where UserId = ? and ReferenceType = ? and Type = ? "
//                    , ORM.getRowMapper()
//                    , userId, referenceType.name(), membershipType
//            );
//            return new HashSet<>(items);
//        } catch (Throwable ex) {
//            logger.error("Failed to find membership by user and reference type and membership type:", ex);
//            throw new TechnicalException("Failed to find membership by user and reference type and membership type", ex);
//        }
//        
//    }

    @Override
    public Set<Membership> findByUserAndReferenceType(String userId, MembershipReferenceType referenceType) throws TechnicalException {

        logger.debug("JdbcMembershipRepository.findByUserAndReferenceType({}, {}, {})", userId, referenceType);
        
        try {
            List<Membership> items = jdbcTemplate.query("select "
                    + " `UserId`, `ReferenceType`, `ReferenceId`, `RoleScope`, `RoleName`, `CreatedAt`, `UpdatedAt` "
                    + " from Membership where UserId = ? and ReferenceType = ? "
                    , ORM.getRowMapper()
                    , userId, referenceType.name()
            );
            return new HashSet<>(items);
        } catch (Throwable ex) {
            logger.error("Failed to find membership by user and membership type:", ex);
            throw new TechnicalException("Failed to find membership by user and membership type", ex);
        }
        
    }
}
