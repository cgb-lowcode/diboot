/*
 * Copyright (c) 2015-2020, www.dibo.ltd (service@dibo.ltd).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.diboot.core.binding.cache;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.diboot.core.binding.parser.EntityInfoCache;
import com.diboot.core.binding.parser.PropInfo;
import com.diboot.core.cache.StaticMemoryCacheManager;
import com.diboot.core.config.Cons;
import com.diboot.core.util.BeanUtils;
import com.diboot.core.util.ContextHelper;
import com.diboot.core.util.S;
import com.diboot.core.util.V;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Primary;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * CacheManager
 * @author mazc@dibo.ltd
 * @version v2.2.1
 * @date 2021/04/17
 */
@SuppressWarnings({"JavaDoc","rawtypes", "unchecked"})
@Slf4j
public class BindingCacheManager {
    /**
     * 实体相关定义缓存管理器
     */
    private static StaticMemoryCacheManager cacheManager;
    /**
     * 类-EntityInfo缓存key
     */
    private static final String CACHE_NAME_CLASS_ENTITY = "CLASS_ENTITY";
    /**
     * 表-EntityInfo缓存key
     */
    private static final String CACHE_NAME_TABLE_ENTITY = "TABLE_ENTITY";
    /**
     * 类-PropInfo缓存key
     */
    private static final String CACHE_NAME_CLASS_PROP = "CLASS_PROP";
    /**
     * Entity类的SimpleName-Entity Class的缓存key
     */
    private static final String CACHE_NAME_ENTITYNAME_CLASS = "NAME_CLASS";
    /**
     * 类-fields缓存
     */
    private static final String CACHE_NAME_CLASS_FIELDS = "CLASS_FIELDS";
    /**
     * 类- name-field Map缓存
     */
    private static final String CACHE_NAME_CLASS_NAME2FLDMAP = "CLASS_NAME2FLDMAP";

    private static synchronized StaticMemoryCacheManager getCacheManager(){
        if(cacheManager == null){
            cacheManager = new StaticMemoryCacheManager(
                    CACHE_NAME_CLASS_ENTITY,
                    CACHE_NAME_TABLE_ENTITY,
                    CACHE_NAME_CLASS_PROP,
                    CACHE_NAME_ENTITYNAME_CLASS,
                    CACHE_NAME_CLASS_FIELDS,
                    CACHE_NAME_CLASS_NAME2FLDMAP);
        }
        return cacheManager;
    }

    /**
     * 根据tableName获取cache
     * @param tableName
     * @return
     */
    public static synchronized EntityInfoCache getEntityInfoByTable(String tableName){
        initEntityInfoCache();
        return getCacheManager().getCacheObj(CACHE_NAME_TABLE_ENTITY, tableName, EntityInfoCache.class);
    }

    /**
     * 根据entity类获取cache
     * @param entityClazz
     * @return
     */
    public static synchronized EntityInfoCache getEntityInfoByClass(Class<?> entityClazz){
        initEntityInfoCache();
        return getCacheManager().getCacheObj(CACHE_NAME_CLASS_ENTITY, entityClazz.getName(), EntityInfoCache.class);
    }

    /**
     * 根据bean类获取bean信息cache
     * @param beanClazz
     * @return
     */
    public static synchronized PropInfo getPropInfoByClass(Class<?> beanClazz){
        PropInfo propInfo = getCacheManager().getCacheObj(CACHE_NAME_CLASS_PROP, beanClazz.getName(), PropInfo.class);
        if(propInfo == null){
            propInfo = initPropInfoCache(beanClazz);
        }
        return propInfo;
    }

    /**
     * 根据tableName获取bean信息cache
     * @param tableName
     * @return
     */
    public static synchronized PropInfo getPropInfoByTable(String tableName){
        Class<?> entityClass = getEntityClassByTable(tableName);
        if(entityClass != null){
            return getPropInfoByClass(entityClass);
        }
        return null;
    }

    /**
     * 根据table名称获取entity类
     * @param tableName
     * @return
     */
    public static synchronized Class<?> getEntityClassByTable(String tableName){
        EntityInfoCache entityInfoCache = getEntityInfoByTable(tableName);
        return entityInfoCache != null? entityInfoCache.getEntityClass() : null;
    }


    /**
     * 根据class simple名称获取entity类
     * @param classSimpleName
     * @return
     */
    public static synchronized Class<?> getEntityClassBySimpleName(String classSimpleName){
        initEntityInfoCache();
        return getCacheManager().getCacheObj(CACHE_NAME_ENTITYNAME_CLASS, classSimpleName, Class.class);
    }

    /**
     * 通过table获取mapper
     * @param table
     * @return
     */
    public static synchronized BaseMapper getMapperByTable(String table){
        EntityInfoCache entityInfoCache = getEntityInfoByTable(table);
        if(entityInfoCache != null){
            return entityInfoCache.getBaseMapper();
        }
        return null;
    }

    /**
     * 通过entity获取mapper
     * @param entityClazz
     * @return
     */
    public static synchronized BaseMapper getMapperByClass(Class<?> entityClazz){
        EntityInfoCache entityInfoCache = getEntityInfoByClass(entityClazz);
        if(entityInfoCache != null){
            return entityInfoCache.getBaseMapper();
        }
        return null;
    }

    /**
     * 获取class的fields
     * @param beanClazz
     * @return
     */
    public static synchronized List<Field> getFields(Class<?> beanClazz){
        List<Field> fields = getCacheManager().getCacheObj(CACHE_NAME_CLASS_FIELDS, beanClazz.getName(), List.class);
        if(fields == null){
            fields = BeanUtils.extractAllFields(beanClazz);
            getCacheManager().putCacheObj(CACHE_NAME_CLASS_FIELDS, beanClazz.getName(), fields);
        }
        return fields;
    }

    /**
     * 获取class中包含指定注解的的fields
     * @param beanClazz
     * @return
     */
    public static synchronized List<Field> getFields(Class<?> beanClazz, Class<? extends Annotation> annotation){
        String key = S.joinWith(Cons.SEPARATOR_COMMA, beanClazz.getName(), annotation.getName());
        List<Field> fields = getCacheManager().getCacheObj(CACHE_NAME_CLASS_FIELDS, key, List.class);
        if(fields == null){
            fields = BeanUtils.extractFields(beanClazz, annotation);
            getCacheManager().putCacheObj(CACHE_NAME_CLASS_FIELDS, key, fields);
        }
        return fields;
    }

    /**
     * 获取class的fields
     * @param beanClazz
     * @return
     */
    public static Map<String, Field> getFieldsMap(Class<?> beanClazz){
        Map<String, Field> fieldsMap = getCacheManager().getCacheObj(CACHE_NAME_CLASS_NAME2FLDMAP, beanClazz.getName(), Map.class);
        if(fieldsMap == null){
            List<Field> fields = getFields(beanClazz);
            fieldsMap = BeanUtils.convertToStringKeyObjectMap(fields, "name");
            getCacheManager().putCacheObj(CACHE_NAME_CLASS_NAME2FLDMAP, beanClazz.getName(), fieldsMap);
        }
        return fieldsMap;
    }

    /**
     * 初始化
     */
    private static void initEntityInfoCache() {
        StaticMemoryCacheManager cacheManager = getCacheManager();
        if (cacheManager.isUninitializedCache(CACHE_NAME_CLASS_ENTITY) == false) {
            return;
        }
        // 初始化有service的entity缓存
        Map<String, IService> serviceMap = ContextHelper.getApplicationContext().getBeansOfType(IService.class);
        Set<String> uniqueEntitySet = new HashSet<>();
        if (V.notEmpty(serviceMap)) {
            for (Map.Entry<String, IService> entry : serviceMap.entrySet()) {
                Class entityClass = BeanUtils.getGenericityClass(entry.getValue(), 1);
                if (entityClass != null) {
                    IService entityIService = entry.getValue();
                    if (uniqueEntitySet.contains(entityClass.getName())) {
                        if (entityIService.getClass().getAnnotation(Primary.class) != null) {
                            EntityInfoCache entityInfoCache = cacheManager.getCacheObj(CACHE_NAME_CLASS_ENTITY, entityClass.getName(), EntityInfoCache.class);
                            if (entityInfoCache != null) {
                                entityInfoCache.setService(entry.getKey());
                            }
                        } else {
                            log.warn("Entity: {} 存在多个service实现类，可能导致调用实例与预期不一致!", entityClass.getName());
                        }
                    } else {
                        EntityInfoCache entityInfoCache = new EntityInfoCache(entityClass, entry.getKey());
                        cacheManager.putCacheObj(CACHE_NAME_CLASS_ENTITY, entityClass.getName(), entityInfoCache);
                        cacheManager.putCacheObj(CACHE_NAME_TABLE_ENTITY, entityInfoCache.getTableName(), entityInfoCache);
                        cacheManager.putCacheObj(CACHE_NAME_ENTITYNAME_CLASS, entityClass.getSimpleName(), entityClass);
                        uniqueEntitySet.add(entityClass.getName());
                    }
                }
            }
        } else {
            log.debug("未获取到任何有效@Service.");
        }
        // 初始化没有service的table-mapper缓存
        SqlSessionFactory sqlSessionFactory = ContextHelper.getBean(SqlSessionFactory.class);
        if (sqlSessionFactory != null) {
            Collection<Class<?>> mappers = sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers();
            if (V.notEmpty(mappers)) {
                for (Class<?> mapperClass : mappers) {
                    Type[] types = mapperClass.getGenericInterfaces();
                    try {
                        if (types.length > 0 && types[0] != null) {
                            ParameterizedType genericType = (ParameterizedType) types[0];
                            Type[] superTypes = genericType.getActualTypeArguments();
                            if (superTypes != null && superTypes.length > 0 && superTypes[0] != null) {
                                String entityClassName = superTypes[0].getTypeName();
                                if (!uniqueEntitySet.contains(entityClassName) && entityClassName.length() > 1) {
                                    Class<?> entityClass = Class.forName(entityClassName);
                                    EntityInfoCache entityInfoCache = new EntityInfoCache(entityClass, null);
                                    entityInfoCache.setBaseMapper((Class<? extends BaseMapper>) mapperClass);
                                    cacheManager.putCacheObj(CACHE_NAME_CLASS_ENTITY, entityClass.getName(), entityInfoCache);
                                    cacheManager.putCacheObj(CACHE_NAME_TABLE_ENTITY, entityInfoCache.getTableName(), entityInfoCache);
                                    cacheManager.putCacheObj(CACHE_NAME_ENTITYNAME_CLASS, entityClass.getSimpleName(), entityClass);
                                    uniqueEntitySet.add(entityClass.getName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析mapper异常", e);
                    }
                }
            }
        }
        uniqueEntitySet = null;
    }

    /**
     * 初始化bean的属性缓存
     * @param beanClazz
     * @return
     */
    private static PropInfo initPropInfoCache(Class<?> beanClazz) {
        PropInfo propInfoCache = new PropInfo(beanClazz);
        getCacheManager().putCacheObj(CACHE_NAME_CLASS_PROP, beanClazz.getName(), propInfoCache);
        return propInfoCache;
    }

}
