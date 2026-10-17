package com.campushub.mapper;

import com.campushub.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    // 根据学号查用户
    User getUserByStudentNo(@Param("studentNo") String studentNo);

    // 根据 id 查用户
    User getUserById(@Param("id") Long id);

    // 注册新增用户
    int insertUser(User user);

    // 明文种子账号登录成功后升级成 BCrypt
    int updatePassword(@Param("id") Long id, @Param("password") String password);

}
