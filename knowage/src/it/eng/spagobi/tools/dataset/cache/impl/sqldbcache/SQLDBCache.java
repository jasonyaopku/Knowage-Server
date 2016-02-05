/**

SpagoBI - The Business Intelligence Free Platform

Copyright (C) 2005-2010 Engineering Ingegneria Informatica S.p.A.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

 **/
package it.eng.spagobi.tools.dataset.cache.impl.sqldbcache;

import it.eng.spagobi.commons.SingletonConfig;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.commons.constants.SpagoBIConstants;
import it.eng.spagobi.tools.dataset.bo.AbstractJDBCDataset;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.cache.CacheException;
import it.eng.spagobi.tools.dataset.cache.ICache;
import it.eng.spagobi.tools.dataset.cache.ICacheActivity;
import it.eng.spagobi.tools.dataset.cache.ICacheEvent;
import it.eng.spagobi.tools.dataset.cache.ICacheListener;
import it.eng.spagobi.tools.dataset.cache.ICacheTrigger;
import it.eng.spagobi.tools.dataset.cache.impl.sqldbcache.work.SQLDBCacheWriteWork;
import it.eng.spagobi.tools.dataset.common.datastore.DataStore;
import it.eng.spagobi.tools.dataset.common.datastore.Field;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.common.datastore.Record;
import it.eng.spagobi.tools.dataset.common.metadata.FieldMetadata;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData.FieldType;
import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
import it.eng.spagobi.tools.dataset.common.metadata.MetaData;
import it.eng.spagobi.tools.dataset.exceptions.ParametersNotValorizedException;
import it.eng.spagobi.tools.dataset.persist.IDataSetTableDescriptor;
import it.eng.spagobi.tools.dataset.persist.PersistedTableManager;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.utilities.Helper;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.cache.CacheItem;
import it.eng.spagobi.utilities.database.temporarytable.TemporaryTableManager;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
import it.eng.spagobi.utilities.locks.DistributedLockFactory;
import it.eng.spagobi.utilities.threadmanager.WorkManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hazelcast.core.IMap;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import commonj.work.Work;
import commonj.work.WorkItem;

/**
 * @author Marco Cortella (marco.cortella@eng.it)
 *
 */
public class SQLDBCache implements ICache {

	private boolean enabled;
	private IDataSource dataSource;

	private UserProfile userProfile;

	private final SQLDBCacheMetadata cacheMetadata;

	private WorkManager spagoBIWorkManager;

	public static final String CACHE_NAME_PREFIX_CONFIG = "SPAGOBI.CACHE.NAMEPREFIX";

	static private Logger logger = Logger.getLogger(SQLDBCache.class);

	public SQLDBCache(SQLDBCacheConfiguration cacheConfiguration) {

		if (cacheConfiguration == null) {
			throw new CacheException("Impossible to initialize cache. The cache configuration object cannot be null");
		}

		this.enabled = true;
		this.dataSource = cacheConfiguration.getCacheDataSource();
		this.cacheMetadata = new SQLDBCacheMetadata(cacheConfiguration);

		this.spagoBIWorkManager = cacheConfiguration.getWorkManager();

		String databaseSchema = cacheConfiguration.getSchema();
		if (databaseSchema != null) {
			// test schema
			testDatabaseSchema(databaseSchema, dataSource);
		}
	}

	// ===================================================================================
	// CONTAINS METHODS
	// ===================================================================================

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#contains(it.eng.spagobi.tools .dataset.bo.IDataSet)
	 */

	@Override
	public boolean contains(IDataSet dataSet) {
		return contains(dataSet.getSignature());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#contains(java.lang.String)
	 */

	@Override
	public boolean contains(String resultsetSignature) {
		return getMetadata().containsCacheItem(resultsetSignature);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#contains(java.util.List)
	 */

	@Override
	public boolean contains(List<IDataSet> dataSets) {
		return getNotContained(dataSets).size() > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#getNotContained(java.util.List)
	 */

	@Override
	public List<IDataSet> getNotContained(List<IDataSet> dataSets) {
		List<IDataSet> notContainedDataSets = new ArrayList<IDataSet>();
		for (IDataSet dataSet : dataSets) {
			if (!contains(dataSet)) {
				notContainedDataSets.add(dataSet);
			}
		}
		return notContainedDataSets;
	}

	// ===================================================================================
	// GET METHODS
	// ===================================================================================

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#get(it.eng.spagobi.tools.dataset. bo.IDataSet)
	 */

	@Override
	public IDataStore get(IDataSet dataSet) {
		IDataStore dataStore = null;

		logger.debug("IN");
		try {
			if (dataSet != null) {
				String dataSetSignature = null;

				try {
					dataSetSignature = dataSet.getSignature();
				} catch (ParametersNotValorizedException p) {
					logger.warn("Error on getting signature for dataset [ " + dataSet.getLabel() + " ]. Error: " + p.getMessage());
					return null; // doesn't cache data
				}
				dataStore = get(dataSetSignature);
			} else {
				logger.warn("Input parameter [dataSet] is null");
			}
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occure while getting dataset from cache", t);
		} finally {
			logger.debug("OUT");
		}

		return dataStore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#get(java.lang.String)
	 */

	@Override
	public IDataStore get(String resultsetSignature) {
		logger.debug("IN");

		IDataStore dataStore = null;

		String hashedSignature = Helper.sha256(resultsetSignature);

		IMap mapLocks = DistributedLockFactory.getDistributedMap(SpagoBIConstants.DISTRIBUTED_MAP_INSTANCE_NAME, SpagoBIConstants.DISTRIBUTED_MAP_FOR_CACHE);
		mapLocks.lock(hashedSignature); // it is possible to use also the method tryLock(...) with timeout parameter
		try {
			if (getMetadata().containsCacheItem(resultsetSignature)) {
				logger.debug("Resultset with signature [" + resultsetSignature + "] found");
				CacheItem cacheItem = getMetadata().getCacheItem(resultsetSignature);
				cacheItem.setLastUsedDate(new Date());
				// update DB information about this cacheItem
				getMetadata().updateCacheItem(cacheItem);
				String tableName = cacheItem.getTable();
				logger.debug("The table associated to dataset [" + resultsetSignature + "] is [" + tableName + "]");
				dataStore = dataSource.executeStatement("SELECT * FROM " + tableName, 0, 0);

				/*
				 * StringBuffer selectBuffer = new StringBuffer(); IDataSetTableDescriptor descriptor = TemporaryTableManager.getTableDescriptor(null,
				 * tableName, dataSource); Set<String> columns = descriptor.getColumnNames(); Iterator<String> it = columns.iterator(); while (it.hasNext()) {
				 * String column = it.next(); if (column.equalsIgnoreCase("sbicache_row_id")) { continue; }
				 * selectBuffer.append(AbstractJDBCDataset.encapsulateColumnAlaias (column, dataSource)); if (it.hasNext()) { selectBuffer.append(", "); } }
				 * String selectClause = selectBuffer.toString(); if (selectClause.endsWith(", ")) { selectClause = selectClause.substring(0,
				 * selectClause.length() - 2); } String sql = "SELECT " + selectClause + " FROM " + tableName; dataStore = dataSource.executeStatement(sql, 0,
				 * 0);
				 */
			} else {
				logger.debug("Resultset with signature [" + resultsetSignature + "] not found");
			}
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occure while getting dataset from cache", t);
		} finally {
			mapLocks.unlock(hashedSignature);
			logger.debug("OUT");
		}

		return dataStore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#get(it.eng.spagobi.tools.dataset. bo.IDataSet, java.util.List, java.util.List, java.util.List)
	 */

	@Override
	public IDataStore get(IDataSet dataSet, List<GroupCriteria> groups, List<FilterCriteria> filters, List<ProjectionCriteria> projections) {
		IDataStore dataStore = null;

		logger.debug("IN");
		try {
			if (dataSet != null) {
				dataStore = getInternal(dataSet, groups, filters, projections);
			} else {
				logger.warn("Input parameter [dataSet] is null");
			}
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occure while getting dataset from cache", t);
		} finally {
			logger.debug("OUT");
		}

		return dataStore;
	}

	public IDataStore getInternal(IDataSet dataSet, List<GroupCriteria> groups, List<FilterCriteria> filters, List<ProjectionCriteria> projections) {
		logger.debug("IN");

		try {

			String resultsetSignature = dataSet.getSignature();
			if (!getMetadata().containsCacheItem(resultsetSignature)) {
				logger.debug("Not found resultSet with signature [" + resultsetSignature + "] inside the Cache");
				return null;
			}
			return queryStandardCachedDataset(groups, filters, projections, resultsetSignature);

		} finally {
			logger.debug("OUT");
		}

	}

	private List<ProjectionCriteria> getProjectionsForInLineView(CacheItem cacheItem, String tableName, String dataSet) {
		IDataSetTableDescriptor descriptor = null;
		try {
			descriptor = TemporaryTableManager.getTableDescriptor(null, tableName, getDataSource());
		} catch (Exception e) {
			throw new SpagoBIRuntimeException("Cannot read columns of table [" + tableName + "]", e);
		}
		Map<String, String> datasetsAlias = (Map<String, String>) cacheItem.getProperty("DATASET_ALIAS");
		Assert.assertNotNull(datasetsAlias, "Datasets' aliases must be specified!!");
		String dataSetAlias = datasetsAlias.get(dataSet);
		Assert.assertNotNull(dataSetAlias, "Dataset's alias must be specified!!");

		List<ProjectionCriteria> toReturn = new ArrayList<ProjectionCriteria>();
		Set<String> columnsName = descriptor.getColumnNames();
		Iterator<String> it = columnsName.iterator();
		while (it.hasNext()) {
			String column = it.next();
			String prefix = dataSetAlias.toUpperCase() + " - ";
			if (column.toUpperCase().startsWith(prefix)) {
				String colunmName = column.substring(prefix.length());
				ProjectionCriteria projection = new ProjectionCriteria(dataSet, colunmName, null, colunmName);
				toReturn.add(projection);
			}
		}
		return toReturn;
	}

	private String getInLineViewSQLDefinition(List<ProjectionCriteria> projections, List<FilterCriteria> filters, CacheItem cacheItem, String tableName) {

		SelectBuilder sqlBuilder = new SelectBuilder();
		sqlBuilder.from(tableName);
		sqlBuilder.setDistinctEnabled(true);

		// Columns to SELECT
		if (projections != null) {
			for (ProjectionCriteria projection : projections) {
				String aggregateFunction = projection.getAggregateFunction();

				Map<String, String> datasetAlias = (Map<String, String>) cacheItem.getProperty("DATASET_ALIAS");
				String columnName = projection.getColumnName();
				if (datasetAlias != null) {
					columnName = datasetAlias.get(projection.getDataset()) + " - " + projection.getColumnName();
				}
				columnName = AbstractJDBCDataset.encapsulateColumnName(columnName, dataSource);

				if ((aggregateFunction != null) && (!aggregateFunction.isEmpty()) && (columnName != "*")) {
					String aliasName = projection.getAliasName();
					aliasName = AbstractJDBCDataset.encapsulateColumnName(aliasName, dataSource);
					if (aliasName != null && !aliasName.isEmpty()) {
						columnName = aggregateFunction + "(" + columnName + ") AS " + aliasName;
					}
				}
				sqlBuilder.column(columnName);

			}
		}

		// WHERE conditions
		if (filters != null) {
			for (FilterCriteria filter : filters) {
				String leftOperand = null;
				if (filter.getLeftOperand().isCostant()) {
					// why? warning!
					leftOperand = filter.getLeftOperand().getOperandValueAsString();
				} else { // it's a column
					Map<String, String> datasetAlias = (Map<String, String>) cacheItem.getProperty("DATASET_ALIAS");
					String datasetLabel = filter.getLeftOperand().getOperandDataSet();
					leftOperand = filter.getLeftOperand().getOperandValueAsString();
					if (datasetAlias != null) {
						if (datasetAlias.get(datasetLabel) == null)
							continue;

						leftOperand = datasetAlias.get(datasetLabel) + " - " + filter.getLeftOperand().getOperandValueAsString();
					}
					leftOperand = AbstractJDBCDataset.encapsulateColumnName(leftOperand, dataSource);
				}

				String operator = filter.getOperator();

				String rightOperand = null;
				if (filter.getRightOperand().isCostant()) {
					if (filter.getRightOperand().isMultivalue()) {
						rightOperand = "(";
						String separator = "";
						String stringDelimiter = "'";
						List<String> values = filter.getRightOperand().getOperandValueAsList();
						for (String value : values) {
							rightOperand += separator + stringDelimiter + value + stringDelimiter;
							separator = ",";
						}
						rightOperand += ")";
					} else {
						rightOperand = filter.getRightOperand().getOperandValueAsString();
					}
				} else { // it's a column
					rightOperand = filter.getRightOperand().getOperandValueAsString();
					rightOperand = AbstractJDBCDataset.encapsulateColumnName(rightOperand, dataSource);
				}

				sqlBuilder.where(leftOperand + " " + operator + " " + rightOperand);
			}
		}

		String inLineViewSQL = sqlBuilder.toString();
		return inLineViewSQL;
	}

	private IDataStore queryStandardCachedDataset(List<GroupCriteria> groups, List<FilterCriteria> filters, List<ProjectionCriteria> projections,
			String resultsetSignature) {

		DataStore toReturn = null;

		String hashedSignature = Helper.sha256(resultsetSignature);

		IMap mapLocks = DistributedLockFactory.getDistributedMap(SpagoBIConstants.DISTRIBUTED_MAP_INSTANCE_NAME, SpagoBIConstants.DISTRIBUTED_MAP_FOR_CACHE);
		mapLocks.lock(hashedSignature); // it is possible to use also the method tryLock(...) with timeout parameter
		try {
			if (getMetadata().containsCacheItem(resultsetSignature)) {
				logger.debug("Found dataset with signature [" + resultsetSignature + "] and hash [" + hashedSignature + "] inside the cache");
				CacheItem cacheItem = getMetadata().getCacheItem(resultsetSignature);
				cacheItem.setLastUsedDate(new Date());
				// update DB information about this cacheItem
				getMetadata().updateCacheItem(cacheItem);
				String tableName = cacheItem.getTable();
				logger.debug("Found resultSet with signature [" + resultsetSignature + "] inside the Cache, table used [" + tableName + "]");

				SelectBuilder sqlBuilder = new SelectBuilder();
				sqlBuilder.from(tableName);

				// Columns to SELECT
				if (projections != null) {
					for (ProjectionCriteria projection : projections) {
						String aggregateFunction = projection.getAggregateFunction();

						Map<String, String> datasetAlias = (Map<String, String>) cacheItem.getProperty("DATASET_ALIAS");
						String columnName = projection.getColumnName();
						if (datasetAlias != null) {
							columnName = datasetAlias.get(projection.getDataset()) + " - " + projection.getColumnName();
						}
						columnName = AbstractJDBCDataset.encapsulateColumnName(columnName, dataSource);

						if ((aggregateFunction != null) && (!aggregateFunction.isEmpty()) && (columnName != "*")) {
							String aliasName = projection.getAliasName();
							aliasName = AbstractJDBCDataset.encapsulateColumnName(aliasName, dataSource);
							if (aliasName != null && !aliasName.isEmpty()) {
								columnName = aggregateFunction + "(" + columnName + ") AS " + aliasName;
							}
						}
						sqlBuilder.column(columnName);

					}
				}

				// WHERE conditions
				if (filters != null) {
					for (FilterCriteria filter : filters) {
						String operator = filter.getOperator();

						String leftOperand = null;
						if (operator.equalsIgnoreCase("IN")) {
							String[] columns = filter.getLeftOperand().getOperandValueAsString().split(",");
							leftOperand = "(1,";
							String separator = "";
							for (String value : columns) {
								leftOperand += separator + AbstractJDBCDataset.encapsulateColumnName(value, dataSource);
								separator = ",";
							}
							leftOperand += ")";
						} else {
							if (filter.getLeftOperand().isCostant()) {
								// why? warning!
								leftOperand = filter.getLeftOperand().getOperandValueAsString();
							} else { // it's a column
								Map<String, String> datasetAlias = (Map<String, String>) cacheItem.getProperty("DATASET_ALIAS");
								String datasetLabel = filter.getLeftOperand().getOperandDataSet();
								leftOperand = filter.getLeftOperand().getOperandValueAsString();
								if (datasetAlias != null) {
									leftOperand = datasetAlias.get(datasetLabel) + " - " + filter.getLeftOperand().getOperandValueAsString();
								}
								leftOperand = AbstractJDBCDataset.encapsulateColumnName(leftOperand, dataSource);
							}
						}

						String rightOperand = null;
						if (filter.getRightOperand().isCostant()) {
							if (filter.getRightOperand().isMultivalue()) {
								rightOperand = "(";
								String separator = "";
								String stringDelimiter = "'";
								List<String> values = filter.getRightOperand().getOperandValueAsList();
								for (String value : values) {
									if (operator.equalsIgnoreCase("IN")) {
										if (value.startsWith(stringDelimiter) && value.endsWith(stringDelimiter)) {
											rightOperand += separator + "(1," + value + ")";
										} else if (value.startsWith("(") && value.endsWith(")")) {
											rightOperand += separator + "(1," + value.substring(1, value.length() - 1) + ")";
										} else {
											rightOperand += separator + "(1," + stringDelimiter + value + stringDelimiter + ")";
										}
									} else {
										rightOperand += separator + stringDelimiter + value + stringDelimiter;
									}
									separator = ",";
								}
								rightOperand += ")";
							} else {
								rightOperand = filter.getRightOperand().getOperandValueAsString();
							}
						} else { // it's a column
							rightOperand = filter.getRightOperand().getOperandValueAsString();
							rightOperand = AbstractJDBCDataset.encapsulateColumnName(rightOperand, dataSource);
						}

						sqlBuilder.where(leftOperand + " " + operator + " " + rightOperand);
					}
				}

				// GROUP BY conditions
				if (groups != null) {
					for (GroupCriteria group : groups) {
						String aggregateFunction = group.getAggregateFunction();

						Map<String, String> datasetAlias = (Map<String, String>) cacheItem.getProperty("DATASET_ALIAS");
						String columnName = group.getColumnName();
						if (datasetAlias != null) {
							columnName = datasetAlias.get(group.getDataset()) + " - " + group.getColumnName();
						}
						columnName = AbstractJDBCDataset.encapsulateColumnName(columnName, dataSource);
						if ((aggregateFunction != null) && (!aggregateFunction.isEmpty()) && (columnName != "*")) {
							columnName = aggregateFunction + "(" + columnName + ")";
						}
						sqlBuilder.groupBy(columnName);
					}
				}

				String queryText = sqlBuilder.toString();
				logger.debug("Cached dataset access query is equal to [" + queryText + "]");

				IDataStore dataStore = dataSource.executeStatement(queryText, 0, 0);
				toReturn = (DataStore) dataStore;

				List<Integer> breakIndexes = (List<Integer>) cacheItem.getProperty("BREAK_INDEXES");
				if (breakIndexes != null) {
					dataStore.getMetaData().setProperty("BREAK_INDEXES", breakIndexes);
				}
			} else {
				logger.debug("Cannot find dataset with signature [" + resultsetSignature + "] and hash [" + hashedSignature + "] inside the cache");
			}
		} finally {
			mapLocks.unlock(hashedSignature);
		}
		return toReturn;
	}

	// ===================================================================================
	// LOAD METHODS
	// ===================================================================================

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#load(it.eng.spagobi.tools.dataset .bo.IDataSet, boolean)
	 */

	@Override
	public IDataStore load(IDataSet dataSet, boolean wait) {
		List<IDataSet> dataSets = new ArrayList<IDataSet>();
		dataSets.add(dataSet);
		List<IDataStore> dataStores = load(dataSets, wait);
		return dataStores.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#load(java.util.List, boolean)
	 */

	@Override
	public List<IDataStore> load(List<IDataSet> dataSets, boolean wait) {
		List<IDataStore> dataStores = new ArrayList<IDataStore>();

		try {
			List<Work> works = new ArrayList<Work>();
			for (IDataSet dataSet : dataSets) {
				// first we set parameters because they change the signature
				// dataSet.setParamsMap(parametersValues);

				IDataStore dataStore = null;

				// then we verified if the store associated to the joined
				// datatset is in cache
				if (contains(dataSet)) {
					dataStore = get(dataSet);
					dataStores.add(dataStore);
					continue;
				}

				// if not we create a work to store it and we add it to works
				// list
				dataSet.loadData();
				dataStore = dataSet.getDataStore();
				dataStores.add(dataStore);

				Work cacheWriteWork = new SQLDBCacheWriteWork(this, dataStore, dataSet, userProfile);
				works.add(cacheWriteWork);
			}

			if (works.size() > 0) {
				if (wait == true) {
					if (spagoBIWorkManager == null) {
						for (int i = 0; i < dataSets.size(); i++) {
							put(dataSets.get(i), dataStores.get(i));
						}
					} else {
						commonj.work.WorkManager workManager = spagoBIWorkManager.getInnerInstance();
						List<WorkItem> workItems = new ArrayList<WorkItem>();
						for (Work work : works) {
							WorkItem workItem = workManager.schedule(work);
							workItems.add(workItem);
						}

						long workTimeout;
						try {
							workTimeout = Long.parseLong(SingletonConfig.getInstance().getConfigValue("SPAGOBI.WORKMANAGER.SQLDBCACHE.TIMEOUT"));
						} catch (NumberFormatException nfe) {
							logger.debug("The value of SPAGOBI.WORKMANAGER.SQLDBCACHE.TIMEOUT config must be an integer");
							workTimeout = commonj.work.WorkManager.INDEFINITE;
						}

						boolean isCompleted = workManager.waitForAll(workItems, workTimeout);
						if (!isCompleted) {
							throw new RuntimeException("Impossible to save the store because the work manager timeout occurred.");
						}
					}
				} else {
					if (spagoBIWorkManager == null) {
						throw new RuntimeException("Impossible to save the store in background because the work manager is not properly initialized");
					}

					commonj.work.WorkManager workManager = spagoBIWorkManager.getInnerInstance();
					for (Work workItem : works) {
						workManager.schedule(workItem);
					}
				}

			}
		} catch (Throwable t) {
			throw new RuntimeException("An unexpected error occured while executing method", t);
		}

		return dataStores;
	}

	// ===================================================================================
	// REFRESH METHODS
	// ===================================================================================

	@Override
	public synchronized void refreshIfNotContained(IDataSet dataSet, boolean wait) {
		if (!contains(dataSet)) {
			refresh(dataSet, wait);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#load(it.eng.spagobi.tools.dataset .bo.IDataSet, boolean)
	 */

	@Override
	public IDataStore refresh(IDataSet dataSet, boolean wait) {

		IDataStore dataStore = null;
		try {
			// If the dataset is a JoinedDataset, then the loadData will take care of creating the temp table for it
			dataSet.loadData();
			dataStore = dataSet.getDataStore();

			if (wait == true) {
				this.put(dataSet, dataStore);
			} else {
				if (spagoBIWorkManager == null) {
					throw new RuntimeException("Impossible to save the store in background because the work manager is not properly initialized");
				}

				commonj.work.WorkManager workManager = spagoBIWorkManager.getInnerInstance();
				Work cacheWriteWork = new SQLDBCacheWriteWork(this, dataStore, dataSet, userProfile);
				workManager.schedule(cacheWriteWork);
			}
		} catch (Throwable t) {
			throw new RuntimeException("An unexpected error occured while executing method", t);
		} finally {
			logger.debug("OUT");
		}

		return dataStore;
	}

	// ===================================================================================
	// PUT METHODS
	// ===================================================================================
	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#put(java.lang.String, it.eng.spagobi.tools.dataset.common.datastore.IDataStore)
	 */

	@Override
	public void put(IDataSet dataSet, IDataStore dataStore) {
		logger.trace("IN");
		String signature = dataSet.getSignature();
		String hashedSignature = Helper.sha256(dataSet.getSignature());

		IMap mapLocks = DistributedLockFactory.getDistributedMap(SpagoBIConstants.DISTRIBUTED_MAP_INSTANCE_NAME, SpagoBIConstants.DISTRIBUTED_MAP_FOR_CACHE);
		mapLocks.lock(hashedSignature); // it is possible to use also the method tryLock(...) with timeout parameter
		try {
			// check again it is not already inserted
			if (getMetadata().containsCacheItem(signature)) {
				logger.debug("Cache item already inserted for dataset with label " + dataSet.getLabel() + " and signature " + dataSet.getSignature());
				return;
			}

			BigDecimal requiredMemory = getMetadata().getRequiredMemory(dataStore);
			BigDecimal maxUsableMemory = getMetadata().getTotalMemory().divide(new BigDecimal(getMetadata().getCachePercentageToStore()), RoundingMode.FLOOR);

			if (requiredMemory.compareTo(maxUsableMemory) < 1) { // if requiredMemory is less or equal to maxUsableMemory

				if (getMetadata().isCleaningEnabled() && !getMetadata().isAvailableMemoryGreaterThen(requiredMemory)) {
					deleteToQuota();
				}

				// check again if the cleaning mechanism is on and if there is enough space for the resultset
				if (!getMetadata().isCleaningEnabled() || getMetadata().isAvailableMemoryGreaterThen(requiredMemory)) {
					String tableName = persistStoreInCache(dataSet, signature, dataStore);
					Map<String, Object> properties = new HashMap<String, Object>();
					List<Integer> breakIndexes = (List<Integer>) dataStore.getMetaData().getProperty("BREAK_INDEXES");
					if (breakIndexes != null) {
						properties.put("BREAK_INDEXES", breakIndexes);
					}
					Map<String, List<String>> columnNames = (Map<String, List<String>>) dataStore.getMetaData().getProperty("COLUMN_NAMES");
					if (columnNames != null) {
						properties.put("COLUMN_NAMES", columnNames);
					}
					// a
					Map<String, String> datasetAlias = (Map<String, String>) dataStore.getMetaData().getProperty("DATASET_ALIAS");
					if (datasetAlias != null) {
						properties.put("DATASET_ALIAS", datasetAlias);
					}

					getMetadata().addCacheItem(signature, properties, tableName, dataStore);

				} else {
					throw new CacheException("Store is to big to be persisted in cache." + " Store estimated dimension is [" + requiredMemory + "]"
							+ " while cache available space is [" + getMetadata().getAvailableMemory() + "]."
							+ " Incrase cache size or execute the dataset disabling cache.");
				}
			} else {
				throw new CacheException("Store is to big to be persisted in cache." + " Store estimated dimension is [" + requiredMemory + "]"
						+ " while the maximum dimension allowed is [" + maxUsableMemory + "]." + " Incrase cache size or execute the dataset disabling cache.");
			}
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occured while adding store into cache", t);
		} finally {
			mapLocks.unlock(hashedSignature);
			logger.trace("OUT");
		}

		logger.debug("OUT");
	}

	private String persistStoreInCache(IDataSet dataset, String signature, IDataStore resultset) {
		logger.trace("IN");
		try {
			int queryTimeout;
			try {
				queryTimeout = Integer.parseInt(SingletonConfig.getInstance().getConfigValue("SPAGOBI.CACHE.CREATE_AND_PERSIST_TABLE.TIMEOUT"));
			} catch (NumberFormatException nfe) {
				logger.debug("The value of SPAGOBI.CACHE.CREATE_AND_PERSIST_TABLE.TIMEOUT config must be an integer");
				queryTimeout = -1;
			}

			PersistedTableManager persistedTableManager = new PersistedTableManager();
			persistedTableManager.setRowCountColumIncluded(true);
			if (queryTimeout > 0) {
				persistedTableManager.setQueryTimeout(queryTimeout);
			}
			String tableName = persistedTableManager.generateRandomTableName(this.getMetadata().getTableNamePrefix());
			Monitor monitor = MonitorFactory.start("spagobi.cache.sqldb.persistStoreInCache.persistdataset");
			persistedTableManager.persistDataset(dataset, resultset, getDataSource(), tableName);
			monitor.stop();
			return tableName;
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occured while persisting store in cache", t);
		} finally {
			logger.trace("OUT");
		}
	}

	// ===================================================================================
	// DELETE METHODS
	// ===================================================================================

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#delete(it.eng.spagobi.tools.dataset .bo.IDataSet)
	 */

	@Override
	public boolean delete(IDataSet dataSet) {
		boolean result = false;

		logger.debug("IN");
		try {
			if (dataSet != null) {
				String dataSetSignature = dataSet.getSignature();
				result = dropTableAndRemoveCacheItem(dataSetSignature, false);
			} else {
				logger.warn("Input parameter [dataSet] is null");
			}
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occure while deleting dataset from cache", t);
		} finally {
			logger.debug("OUT");
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#delete(java.lang.String)
	 */

	@Override
	public boolean delete(String signature) {
		return delete(signature, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#delete(java.lang.String)
	 */
	private boolean delete(String signature, boolean isHash) {
		boolean result = false;

		logger.debug("IN");
		try {
			if (signature != null) {
				result = dropTableAndRemoveCacheItem(signature, isHash);
			} else {
				logger.warn("Input parameter [" + signature + "] is null");
			}
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occure while deleting dataset [" + signature + "] from cache", t);
		} finally {
			logger.debug("OUT");
		}

		return result;
	}

	private boolean dropTableAndRemoveCacheItem(String signature, boolean isHash) {
		logger.debug("IN");
		String hashedSignature;
		if (isHash) {
			hashedSignature = signature;
			logger.debug("Delete dataset with hash [" + signature + "]");
		} else {
			hashedSignature = Helper.sha256(signature);
			logger.debug("Delete dataset with signature [" + signature + "] and hash [" + hashedSignature + "]");
		}
		IMap mapLocks = DistributedLockFactory.getDistributedMap(SpagoBIConstants.DISTRIBUTED_MAP_INSTANCE_NAME, SpagoBIConstants.DISTRIBUTED_MAP_FOR_CACHE);
		mapLocks.lock(hashedSignature); // it is possible to use also the method tryLock(...) with timeout parameter
		try {
			if (getMetadata().containsCacheItem(signature, isHash)) {
				PersistedTableManager persistedTableManager = new PersistedTableManager();
				String tableName = getMetadata().getCacheItem(signature, isHash).getTable();
				persistedTableManager.dropTableIfExists(getDataSource(), tableName);
				getMetadata().removeCacheItem(signature, isHash);
				logger.debug("Removed table " + tableName + " from [SQLDBCache] corresponding to the result Set: " + signature);
				logger.debug("Deleted");

				return true;
			} else {
				logger.debug("Not deleted, dataset not in cache");
				return false;
			}
		} finally {
			mapLocks.unlock(hashedSignature);
			logger.debug("OUT");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#deleteQuota()
	 */

	@Override
	public void deleteToQuota() {
		logger.trace("IN");
		boolean isEnough = false;
		try {
			List<CacheItem> items = getMetadata().getCacheItems();
			for (CacheItem item : items) {
				long elapsedTime = (System.currentTimeMillis() - item.getLastUsedDate().getTime()) / 1000;
				if (elapsedTime > getMetadata().getCacheDsLastAccessTtl()) {
					delete(item.getSignature(), true);
					if (getMetadata().getAvailableMemoryAsPercentage() > getMetadata().getCleaningQuota()) {
						isEnough = true;
						break;
					}
				}
			}
			// Second loop through datasets
			if (!isEnough) {
				for (String signature : getMetadata().getSignatures()) {
					delete(signature, true);
					if (getMetadata().getAvailableMemoryAsPercentage() > getMetadata().getCleaningQuota()) {
						break;
					}
				}
			}
		} catch (Throwable t) {
			if (t instanceof CacheException)
				throw (CacheException) t;
			else
				throw new CacheException("An unexpected error occured while deleting cache to quota", t);
		} finally {
			logger.trace("OUT");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#deleteAll()
	 */

	@Override
	public void deleteAll() {
		logger.debug("Removing all tables from [SQLDBCache]");

		List<String> signatures = getMetadata().getSignatures();
		for (String signature : signatures) {
			delete(signature, true);
		}
		// Delete any other cache tables, even if not recorded as cache item
		// eraseExistingTables(getMetadata().getTableNamePrefix().toUpperCase());
		logger.debug("[SQLDBCache] All tables removed, Cache cleaned ");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#deleteOnlyStale()
	 */
	public void deleteOnlyStale() {
		logger.debug("Removing all stale tables from [SQLDBCache]");

		List<CacheItem> items = getMetadata().getCacheItems();
		for (CacheItem item : items) {
			long elapsedTime = (System.currentTimeMillis() - item.getLastUsedDate().getTime()) / 1000;
			if (elapsedTime > getMetadata().getCacheDsLastAccessTtl()) {
				delete(item.getSignature(), true);
			}
		}
		logger.debug("[SQLDBCache] All stale tables removed, Cache cleaned ");
	}

	/**
	 * Erase existing tables that begins with the prefix
	 *
	 * @param prefix
	 *            table name prefix
	 *
	 */
	private void eraseExistingTables(String prefix) {
		PersistedTableManager persistedTableManager = new PersistedTableManager();
		persistedTableManager.dropTablesWithPrefix(getDataSource(), prefix);
	}

	// ===================================================================================
	// ACCESSOR METHODS
	// ===================================================================================

	/**
	 * @return the dataSource
	 */
	public IDataSource getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource
	 *            the dataSource to set
	 */
	public void setDataSource(IDataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Test if the passed schema name is correct. Create a table in the database via the dataSource then try to select the table using the schema.table syntax
	 *
	 * @param schema
	 *            the schema name
	 * @param dataSource
	 *            the DataSource
	 */
	private void testDatabaseSchema(String schema, IDataSource dataSource) {

		// Create a fake dataStore
		DataStore dataStore = new DataStore();
		IMetaData metadata = new MetaData();
		IFieldMetaData fieldMetaData = new FieldMetadata();
		fieldMetaData.setAlias("test_column");
		fieldMetaData.setName("test_column");
		fieldMetaData.setType(String.class);
		fieldMetaData.setFieldType(FieldType.ATTRIBUTE);
		metadata.addFiedMeta(fieldMetaData);
		dataStore.setMetaData(metadata);
		Record record = new Record();
		Field field = new Field();
		field.setValue("try");
		record.appendField(field);
		dataStore.appendRecord(record);

		// persist the datastore as a table on db
		String dialect = dataSource.getHibDialectClass();
		PersistedTableManager persistedTableManager = new PersistedTableManager();
		persistedTableManager.setDialect(dialect);
		Random ran = new Random();
		int x = ran.nextInt(100);
		String tableName = "SbiTest" + x;
		persistedTableManager.setTableName(tableName);

		try {
			persistedTableManager.persistDataset(dataStore, dataSource);
		} catch (Exception e) {
			logger.error("Error persisting dataset", e);
		}

		// try to query the table using the Schema.TableName syntax if
		// schemaName is valorized

		try {
			if (schema.isEmpty()) {
				dataSource.executeStatement("SELECT * FROM " + tableName, 0, 0);

			} else {
				dataSource.executeStatement("SELECT * FROM " + schema + "." + tableName, 0, 0);
			}
		} catch (Exception e) {
			throw new CacheException("An unexpected error occured while testing database schema for cache", e);
		} finally {
			// Dropping table
			persistedTableManager.dropTableIfExists(dataSource, tableName);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.dataset.cache.ICache#getCacheMetadata()
	 */

	@Override
	public SQLDBCacheMetadata getMetadata() {
		return cacheMetadata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#addListener(it.eng.spagobi. tools.dataset.cache.ICacheEvent,
	 * it.eng.spagobi.tools.dataset.cache.ICacheListener)
	 */

	@Override
	public void addListener(ICacheEvent event, ICacheListener listener) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#scheduleActivity(it.eng.spagobi .tools.dataset.cache.ICacheActivity,
	 * it.eng.spagobi.tools.dataset.cache.ICacheTrigger)
	 */

	@Override
	public void scheduleActivity(ICacheActivity activity, ICacheTrigger trigger) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#enable(boolean)
	 */

	@Override
	public void enable(boolean enable) {
		this.enabled = enable;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#isEnabled()
	 */

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public WorkManager getSpagoBIWorkManager() {
		return spagoBIWorkManager;
	}

	public void setSpagoBIWorkManager(WorkManager spagoBIWorkManager) {
		this.spagoBIWorkManager = spagoBIWorkManager;
	}

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public void setUserProfile(UserProfile userProfile) {
		this.userProfile = userProfile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.eng.spagobi.tools.dataset.cache.ICache#refresh(java.util.List, boolean)
	 */

	@Override
	public IDataStore refresh(List<IDataSet> dataSets, boolean wait) {
		// TODO Auto-generated method stub
		return null;
	}

}
