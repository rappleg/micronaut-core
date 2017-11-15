package org.particleframework.jdbc;

import org.particleframework.core.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public enum DatabaseDriver {

    /**
     * Unknown type.
     */
    UNKNOWN(null, null),

    /**
     * Apache Derby.
     */
    DERBY("Apache Derby", "org.apache.derby.jdbc.EmbeddedDriver",
            "org.apache.derby.jdbc.EmbeddedXADataSource",
            "SELECT 1 FROM SYSIBM.SYSDUMMY1"),

    /**
     * H2.
     */
    H2("H2", "org.h2.Driver", "org.h2.jdbcx.JdbcDataSource", "SELECT 1"),

    /**
     * HyperSQL DataBase.
     */
    HSQLDB("HSQL Database Engine", "org.hsqldb.jdbc.JDBCDriver",
            "org.hsqldb.jdbc.pool.JDBCXADataSource",
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SYSTEM_USERS"),

    /**
     * SQL Lite.
     */
    SQLITE("SQLite", "org.sqlite.JDBC"),

    /**
     * MySQL.
     */
    MYSQL("MySQL", "com.mysql.jdbc.Driver",
            "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource", "SELECT 1"),

    /**
     * Maria DB.
     */
    MARIADB("MySQL", "org.mariadb.jdbc.Driver", "org.mariadb.jdbc.MariaDbDataSource",
            "SELECT 1") {

        @Override
        public String getId() {
            return "mysql";
        }
    },

    /**
     * Google App Engine.
     */
    GAE(null, "com.google.appengine.api.rdbms.AppEngineDriver"),

    /**
     * Oracle.
     */
    ORACLE("Oracle", "oracle.jdbc.OracleDriver",
            "oracle.jdbc.xa.client.OracleXADataSource", "SELECT 'Hello' from DUAL"),

    /**
     * Postgres.
     */
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "org.postgresql.xa.PGXADataSource",
            "SELECT 1"),

    /**
     * jTDS. As it can be used for several databases, there isn't a single product name we
     * could rely on.
     */
    JTDS(null, "net.sourceforge.jtds.jdbc.Driver"),

    /**
     * SQL Server.
     */
    SQLSERVER("Microsoft SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "com.microsoft.sqlserver.jdbc.SQLServerXADataSource", "SELECT 1") {

        @Override
        protected boolean matchProductName(String productName) {
            return super.matchProductName(productName)
                    || "SQL SERVER".equalsIgnoreCase(productName);

        }
    },

    /**
     * Firebird.
     */
    FIREBIRD("Firebird", "org.firebirdsql.jdbc.FBDriver",
            "org.firebirdsql.ds.FBXADataSource",
            "SELECT 1 FROM RDB$DATABASE") {

        @Override
        protected Collection<String> getUrlPrefixes() {
            return Collections.singleton("firebirdsql");
        }

        @Override
        protected boolean matchProductName(String productName) {
            return super.matchProductName(productName)
                    || productName.toLowerCase().startsWith("firebird");
        }
    },

    /**
     * DB2 Server.
     */
    DB2("DB2", "com.ibm.db2.jcc.DB2Driver", "com.ibm.db2.jcc.DB2XADataSource",
            "SELECT 1 FROM SYSIBM.SYSDUMMY1") {

        @Override
        protected boolean matchProductName(String productName) {
            return super.matchProductName(productName)
                    || productName.toLowerCase().startsWith("db2/");
        }
    },

    /**
     * DB2 AS400 Server.
     */
    DB2_AS400("DB2 UDB for AS/400", "com.ibm.as400.access.AS400JDBCDriver",
            "com.ibm.as400.access.AS400JDBCXADataSource",
            "SELECT 1 FROM SYSIBM.SYSDUMMY1") {

        @Override
        public String getId() {
            return "db2";
        }

        @Override
        protected Collection<String> getUrlPrefixes() {
            return Collections.singleton("as400");
        }

        @Override
        protected boolean matchProductName(String productName) {
            return super.matchProductName(productName)
                    || productName.toLowerCase().contains("as/400");
        }
    },

    /**
     * Teradata.
     */
    TERADATA("Teradata", "com.teradata.jdbc.TeraDriver"),

    /**
     * Informix.
     */
    INFORMIX("Informix Dynamic Server", "com.informix.jdbc.IfxDriver", null,
            "select count(*) from systables") {

        @Override
        protected Collection<String> getUrlPrefixes() {
            return Arrays.asList("informix-sqli", "informix-direct");
        }

    };

    private final String productName;

    private final String driverClassName;

    private final String xaDataSourceClassName;

    private final String validationQuery;

    DatabaseDriver(String productName, String driverClassName) {
        this(productName, driverClassName, null);
    }

    DatabaseDriver(String productName, String driverClassName,
                   String xaDataSourceClassName) {
        this(productName, driverClassName, xaDataSourceClassName, null);
    }

    DatabaseDriver(String productName, String driverClassName,
                   String xaDataSourceClassName, String validationQuery) {
        this.productName = productName;
        this.driverClassName = driverClassName;
        this.xaDataSourceClassName = xaDataSourceClassName;
        this.validationQuery = validationQuery;
    }

    /**
     * Return the identifier of this driver.
     * @return the identifier
     */
    public String getId() {
        return name().toLowerCase();
    }

    protected boolean matchProductName(String productName) {
        return this.productName != null && this.productName.equalsIgnoreCase(productName);
    }

    protected Collection<String> getUrlPrefixes() {
        return Collections.singleton(this.name().toLowerCase());
    }

    /**
     * Return the driver class name.
     * @return the class name or {@code null}
     */
    public String getDriverClassName() {
        return this.driverClassName;
    }

    /**
     * Return the XA driver source class name.
     * @return the class name or {@code null}
     */
    public String getXaDataSourceClassName() {
        return this.xaDataSourceClassName;
    }

    /**
     * Return the validation query.
     * @return the validation query or {@code null}
     */
    public String getValidationQuery() {
        return this.validationQuery;
    }

    /**
     * Find a {@link DatabaseDriver} for the given URL.
     * @param url JDBC URL
     * @return the database driver or {@link #UNKNOWN} if not found
     */
    /**
     * Find a {@link DatabaseDriver} for the given URL.
     * @param url JDBC URL
     * @return the database driver or {@link #UNKNOWN} if not found
     */
    public static DatabaseDriver fromJdbcUrl(String url) {
        if (StringUtils.isNotEmpty(url)) {
            if (!url.startsWith("jdbc")) {
                throw new IllegalArgumentException("JDBC URLs must start with 'jdbc'");
            }
            String urlWithoutPrefix = url.substring("jdbc".length()).toLowerCase();
            for (DatabaseDriver driver : values()) {
                for (String urlPrefix : driver.getUrlPrefixes()) {
                    String prefix = ":" + urlPrefix + ":";
                    if (driver != UNKNOWN && urlWithoutPrefix.startsWith(prefix)) {
                        return driver;
                    }
                }
            }
        }
        return UNKNOWN;
    }

}