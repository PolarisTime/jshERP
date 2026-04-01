package com.jsh.erp.datasource.mappers;

import org.apache.ibatis.annotations.Param;

public interface SequenceMapperEx {

    void updateBuildOnlyNumber();

    /**
     * 获得一个全局唯一的数作为订单号的追加
     * */
    Long getBuildOnlyNumber(@Param("seq_name") String seq_name);

    /**
     * 按序列名递增 current_val
     */
    void incrementBySeqName(@Param("seqName") String seqName);

    /**
     * 按序列名获取 current_val
     */
    Long getValBySeqName(@Param("seqName") String seqName);

    /**
     * 按序列名重置 current_val
     */
    void resetBySeqName(@Param("seqName") String seqName, @Param("val") long val);
}
