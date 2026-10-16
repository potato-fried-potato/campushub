package com.campushub.mapper;

import com.campushub.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    // 根据学号查用户
    User getUserByStudentNo(@Param("studentNo") String studentNo);

}
