package com.campushub.mapper;

import com.campushub.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    int insert(Report report);

    int exists(@Param("itemId") Long itemId, @Param("reporterId") Long reporterId);

    Report getById(@Param("id") Long id);

    List<Report> list(@Param("status") String status,
                      @Param("offset") int offset,
                      @Param("pageSize") int pageSize);

    int count(@Param("status") String status);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("handledBy") Long handledBy);

}
