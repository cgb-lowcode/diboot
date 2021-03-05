package com.diboot.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.diboot.core.config.Cons;
import com.diboot.core.util.BeanUtils;
import com.diboot.core.util.ContextHelper;
import com.diboot.core.util.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Entity抽象父类
 * @author Yangzhao@dibo.ltd
 * @version v2.0
 * Copyright © diboot.com
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class AbstractEntity<T> implements Serializable {
    private static final long serialVersionUID = 10202L;

    /**
     * 默认主键字段id，类型为Long型自增，转json时转换为String
     */
    @TableId(type = IdType.AUTO)
    private T id;

    /**
     * Entity对象转为String
     * @return
     */
    @Override
    public String toString(){
        return this.getClass().getName()+ ":"+this.getId();
    }
}
