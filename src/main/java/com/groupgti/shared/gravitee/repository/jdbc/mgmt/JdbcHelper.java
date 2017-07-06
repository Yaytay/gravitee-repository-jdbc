/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.mgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;


/**
 *
 * @author njt
 */
public class JdbcHelper {
    
    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcHelper.class);
    
    @FunctionalInterface
    public static interface ChildAdder<T> {
        
        void addChild(T parent, ResultSet rs) throws SQLException;
        
    }
    
    public static class CollatingRowMapper<T> implements RowCallbackHandler {

        private final RowMapper<T> mapper;
        private final ChildAdder<T> childAdder;
        private final String idColumn;
        private final List<T> rows;
        private Comparable lastId;
        private T current;

        public CollatingRowMapper(RowMapper mapper, ChildAdder<T> childAdder, String idColumn) {
            this.mapper = mapper;
            this.childAdder = childAdder;
            this.idColumn = idColumn;
            this.rows = new ArrayList<>();
        }
        
        @Override
        public void processRow(ResultSet rs) throws SQLException {
            
            Comparable currentId = (Comparable)rs.getObject(idColumn);
            if ((lastId == null) || (lastId.compareTo(currentId) != 0)) {
                lastId = currentId;
                current = mapper.mapRow(rs, rows.size() + 1);
                rows.add(current);
            }
            childAdder.addChild(current, rs);
            
        }

        public List<T> getRows() {
            return rows;
        }
            
    }

}
