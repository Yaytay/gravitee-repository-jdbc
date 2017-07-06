/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcColumn;
import com.groupgti.shared.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageConfiguration;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.repository.management.model.PageType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcPageRepository extends JdbcAbstractCrudRepository<Page, String> implements PageRepository {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcPageRepository.class);
    
    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Page.class, "Id")
            .addColumn("Id", Types.NVARCHAR, String.class)
            .addColumn("Type", Types.NVARCHAR, PageType.class)
            .addColumn("Name", Types.NVARCHAR, String.class)
            .addColumn("Content", Types.NVARCHAR, String.class)
            .addColumn("LastContributor", Types.NVARCHAR, String.class)
            .addColumn("Order", Types.INTEGER, int.class)
            .addColumn("Api", Types.NVARCHAR, String.class)
            .addColumn("Published", Types.BIT, boolean.class)
            .addColumn("Homepage", Types.BIT, boolean.class)
//            .addColumn("SourceType", Types.NVARCHAR, String.class)
//            .addColumn("SourceConfiguration", Types.NVARCHAR, String.class)
//            .addColumn("ConfigurationTryItURL", Types.NVARCHAR, String.class)
//            .addColumn("ConfigurationTryIt", Types.BIT, boolean.class)
            .addColumn("CreatedAt", Types.TIMESTAMP, Date.class)
            .addColumn("UpdatedAt", Types.TIMESTAMP, Date.class)
            .build();    
    
    private static class Rm implements RowMapper<Page> {
        
        @Override
        public Page mapRow(ResultSet rs, int i) throws SQLException {
            Page page = new Page();
            ORM.setFromResultSet(page, rs);
            String sourceType = rs.getString("SourceType");
            String sourceConfiguration = rs.getString("SourceConfiguration");
            if ((sourceType != null) || (sourceConfiguration != null)) {
                PageSource pageSource = new PageSource();
                pageSource.setType(sourceType);
                pageSource.setConfiguration(sourceConfiguration);
                page.setSource(pageSource);
            }
            PageConfiguration pageConfiguration = new PageConfiguration();
            pageConfiguration.setTryIt(rs.getBoolean("ConfigurationTryIt"));
            pageConfiguration.setTryItURL(rs.getString("ConfigurationTryItURL"));
            page.setConfiguration(pageConfiguration);
            return page;
        }
    };
    
    private static final Rm MAPPER = new Rm();
    
    private static class Psc implements PreparedStatementCreator {

        private final String sql;
        private final Page page;
        private final Object[] ids;

        public Psc(String sql, Page page, Object... ids) {
            this.sql = sql;
            this.page = page;
            this.ids = ids;
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection cnctn) throws SQLException {
            logger.debug("SQL: {}", sql);
            logger.debug("Page: {}", page);
            PreparedStatement stmt = cnctn.prepareStatement(sql);
            int idx = ORM.setStatementValues(stmt, page, 1);
            stmt.setString(idx++, page.getSource() == null ? null : page.getSource().getType());
            stmt.setString(idx++, page.getSource() == null ? null : page.getSource().getConfiguration());
            stmt.setBoolean(idx++, page.getConfiguration() == null ? false : page.getConfiguration().isTryIt());
            stmt.setString(idx++, page.getConfiguration() == null ? null : page.getConfiguration().getTryItURL());
            
            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }
            return stmt;
        }
        
    };
        
    
    private static String buildInsertStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into Page (");
        boolean first = true;
        for (JdbcColumn column : (List<JdbcColumn>) ORM.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append('`').append(column.name).append('`');
        }
        builder.append(", `SourceType`");
        builder.append(", `SourceConfiguration`");
        builder.append(", `ConfigurationTryIt`");
        builder.append(", `ConfigurationTryItURL`");
        builder.append(" ) values ( ");
        first = true;
        for (JdbcColumn column : (List<JdbcColumn>) ORM.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("?");
        }
        builder.append(", ?");
        builder.append(", ?");
        builder.append(", ?");
        builder.append(", ?");
        builder.append(" )");
        return builder.toString();
    }
    
    private static final String INSERT_SQL = buildInsertStatement();
    
    private static String buildUpdateStatement() {
        StringBuilder builder = new StringBuilder();
        builder.append("update Page set ");
        boolean first = true;
        for (JdbcColumn column : (List<JdbcColumn>) ORM.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append('`').append(column.name).append('`');
            builder.append(" = ?");
        }
        builder.append(", `SourceType` = ?");
        builder.append(", `SourceConfiguration` = ?");
        builder.append(", `ConfigurationTryIt` = ?");
        builder.append(", `ConfigurationTryItURL` = ?");
        
        builder.append(" where Id = ?");
        return builder.toString();
    }
    
    private static final String UPDATE_SQL = buildUpdateStatement();
    
    
    private final JdbcTemplate jdbcTemplate;
    
    public JdbcPageRepository(DataSource dataSource) {
        super(dataSource, Page.class);
        logger.debug("JdbcPageRepository()");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    protected JdbcObjectMapper getOrm() {
        return ORM;
    }

    @Override
    protected String getId(Page item) {
        return item.getId();
    }

    @Override
    protected RowMapper<Page> getRowMapper() {
        return MAPPER;
    }

    @Override
    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(Page page) {
        return new Psc(UPDATE_SQL, page, page.getId());
    }

    @Override
    protected PreparedStatementCreator buildInsertPreparedStatementCreator(Page page) {
        return new Psc(INSERT_SQL, page);
    }

    @Override
    public Collection<Page> findApiPageByApiIdAndHomepage(String apiId, boolean homePage) throws TechnicalException {
        logger.debug("JdbcPageRepository.findApiPageByApiIdAndHomepage({}, {})", apiId, homePage);
        
        try {
            List<Page> items = jdbcTemplate.query("select * from Page where Api = ? and HomePage = ? order by `Order`"
                    , getRowMapper()
                    , apiId, homePage
            );
            return items;
        } catch (Throwable ex) {
            logger.error("Failed to find page by api and homepage:", ex);
            throw new TechnicalException("Failed to find page by api and homepage", ex);
        }
    }

    @Override
    public Collection<Page> findApiPageByApiId(String apiId) throws TechnicalException {

        logger.debug("JdbcPageRepository.findApiPageByApiId({})", apiId);
        
        try {
            List<Page> items = jdbcTemplate.query("select * from Page where Api = ? order by `Order`"
                    , getRowMapper()
                    , apiId
            );
            return items;
        } catch (Throwable ex) {
            logger.error("Failed to find page by api:", ex);
            throw new TechnicalException("Failed to find page by api", ex);
        }
        
    }
    
    @Override
    public Integer findMaxApiPageOrderByApiId(String apiId) throws TechnicalException {

        logger.debug("JdbcPageRepository.findMaxApiPageOrderByApiId({})", apiId);
        
        try {
            Integer result = jdbcTemplate.queryForObject("select max(`Order`) from Page where Api = ? "
                    , Integer.class
                    , apiId
            );
            
            return result == null ? 0 : result;
        } catch (Throwable ex) {
            logger.error("Failed to find max page order by api:", ex);
            throw new TechnicalException("Failed to find max page order by api", ex);
        }
        
    }

    @Override
    public Collection<Page> findPortalPageByHomepage(boolean homePage) throws TechnicalException {

        logger.debug("JdbcPageRepository.findPortalPageByHomepage({})", homePage);
        
        try {
            List<Page> items = jdbcTemplate.query("select * from Page where Api is null and HomePage = ? order by `Order`"
                    , getRowMapper()
                    , homePage
            );
            return items;
        } catch (Throwable ex) {
            logger.error("Failed to find page by api:", ex);
            throw new TechnicalException("Failed to find page by api", ex);
        }
        
    }

    @Override
    public Collection<Page> findPortalPages() throws TechnicalException {

        logger.debug("JdbcPageRepository.findPortalPages()");
        
        try {
            List<Page> items = jdbcTemplate.query("select * from Page where Api is null order by `Order`"
                    , getRowMapper()
            );
            return items;
        } catch (Throwable ex) {
            logger.error("Failed to find page by api:", ex);
            throw new TechnicalException("Failed to find page by api", ex);
        }
        
    }

    @Override
    public Integer findMaxPortalPageOrder() throws TechnicalException {

        logger.debug("JdbcPageRepository.findMaxPortalPageOrder()");
        
        try {
            Integer result = jdbcTemplate.queryForObject("select max(`Order`) from Page where Api is null "
                    , Integer.class
            );
            return result;
        } catch (Throwable ex) {
            logger.error("Failed to find page by api:", ex);
            throw new TechnicalException("Failed to find page by api", ex);
        }
        
    }
        
}
