package com.ems;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Wraps a shared Connection so that close() and setAutoCommit() calls from
 * service code don't affect the underlying in-memory connection between tests.
 */
public class NonClosingConnection implements Connection {
    private final Connection delegate;

    public NonClosingConnection(Connection delegate) {
        this.delegate = delegate;
    }

    @Override public void close() { /* no-op — keep shared connection alive */ }
    @Override public void setAutoCommit(boolean a) throws SQLException { delegate.setAutoCommit(a); }
    @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
    @Override public void commit() throws SQLException { delegate.commit(); }
    @Override public void rollback() throws SQLException { delegate.rollback(); }
    @Override public void rollback(Savepoint s) throws SQLException { delegate.rollback(s); }

    // ── Delegate everything else ──────────────────────────────────────────────
    @Override public PreparedStatement prepareStatement(String s) throws SQLException { return delegate.prepareStatement(s); }
    @Override public PreparedStatement prepareStatement(String s, int a) throws SQLException { return delegate.prepareStatement(s, a); }
    @Override public PreparedStatement prepareStatement(String s, int[] a) throws SQLException { return delegate.prepareStatement(s, a); }
    @Override public PreparedStatement prepareStatement(String s, String[] a) throws SQLException { return delegate.prepareStatement(s, a); }
    @Override public PreparedStatement prepareStatement(String s, int a, int b) throws SQLException { return delegate.prepareStatement(s, a, b); }
    @Override public PreparedStatement prepareStatement(String s, int a, int b, int c) throws SQLException { return delegate.prepareStatement(s, a, b, c); }
    @Override public CallableStatement prepareCall(String s) throws SQLException { return delegate.prepareCall(s); }
    @Override public CallableStatement prepareCall(String s, int a, int b) throws SQLException { return delegate.prepareCall(s, a, b); }
    @Override public CallableStatement prepareCall(String s, int a, int b, int c) throws SQLException { return delegate.prepareCall(s, a, b, c); }
    @Override public Statement createStatement() throws SQLException { return delegate.createStatement(); }
    @Override public Statement createStatement(int a, int b) throws SQLException { return delegate.createStatement(a, b); }
    @Override public Statement createStatement(int a, int b, int c) throws SQLException { return delegate.createStatement(a, b, c); }
    @Override public String nativeSQL(String s) throws SQLException { return delegate.nativeSQL(s); }
    @Override public boolean isClosed() throws SQLException { return delegate.isClosed(); }
    @Override public DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
    @Override public void setReadOnly(boolean b) throws SQLException { delegate.setReadOnly(b); }
    @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
    @Override public void setCatalog(String s) throws SQLException { delegate.setCatalog(s); }
    @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
    @Override public void setTransactionIsolation(int i) throws SQLException { delegate.setTransactionIsolation(i); }
    @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
    @Override public SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
    @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
    @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
    @Override public void setTypeMap(Map<String, Class<?>> m) throws SQLException { delegate.setTypeMap(m); }
    @Override public void setHoldability(int h) throws SQLException { delegate.setHoldability(h); }
    @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
    @Override public Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
    @Override public Savepoint setSavepoint(String s) throws SQLException { return delegate.setSavepoint(s); }
    @Override public void releaseSavepoint(Savepoint s) throws SQLException { delegate.releaseSavepoint(s); }
    @Override public Clob createClob() throws SQLException { return delegate.createClob(); }
    @Override public Blob createBlob() throws SQLException { return delegate.createBlob(); }
    @Override public NClob createNClob() throws SQLException { return delegate.createNClob(); }
    @Override public SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
    @Override public boolean isValid(int t) throws SQLException { return delegate.isValid(t); }
    @Override public void setClientInfo(String n, String v) throws SQLClientInfoException { try { delegate.setClientInfo(n, v); } catch (SQLClientInfoException e) { throw e; } }
    @Override public void setClientInfo(Properties p) throws SQLClientInfoException { try { delegate.setClientInfo(p); } catch (SQLClientInfoException e) { throw e; } }
    @Override public String getClientInfo(String n) throws SQLException { return delegate.getClientInfo(n); }
    @Override public Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
    @Override public Array createArrayOf(String t, Object[] e) throws SQLException { return delegate.createArrayOf(t, e); }
    @Override public Struct createStruct(String t, Object[] a) throws SQLException { return delegate.createStruct(t, a); }
    @Override public void setSchema(String s) throws SQLException { delegate.setSchema(s); }
    @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
    @Override public void abort(Executor e) throws SQLException { delegate.abort(e); }
    @Override public void setNetworkTimeout(Executor e, int ms) throws SQLException { delegate.setNetworkTimeout(e, ms); }
    @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
    @Override public <T> T unwrap(Class<T> i) throws SQLException { return delegate.unwrap(i); }
    @Override public boolean isWrapperFor(Class<?> i) throws SQLException { return delegate.isWrapperFor(i); }
}
