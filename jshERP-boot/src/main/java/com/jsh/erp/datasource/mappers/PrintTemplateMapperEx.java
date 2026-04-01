package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.PrintTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 打印模板Mapper接口
 */
public interface PrintTemplateMapperEx {

    PrintTemplate selectDefaultByBillType(@Param("billType") String billType);

    List<PrintTemplate> selectListByBillType(@Param("billType") String billType);

    int insertTemplate(@Param("billType") String billType,
                       @Param("templateName") String templateName,
                       @Param("templateHtml") String templateHtml,
                       @Param("isDefault") String isDefault);

    int updateById(@Param("id") Long id,
                   @Param("templateName") String templateName,
                   @Param("templateHtml") String templateHtml,
                   @Param("isDefault") String isDefault);

    int deleteById(@Param("id") Long id);

    int clearDefaultByBillType(@Param("billType") String billType, @Param("excludeId") Long excludeId);
}
