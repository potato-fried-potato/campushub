package com.campushub.mapper;

import com.campushub.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    // 管理后台：用户列表
    List<User> listForAdmin(@Param("keyword") String keyword,
                            @Param("status") String status,
                            @Param("offset") int offset,
                            @Param("pageSize") int pageSize);

    int countForAdmin(@Param("keyword") String keyword, @Param("status") String status);

    // 管理后台：启用 / 禁用账号
    int updateStatus(@Param("id") Long id, @Param("status") String status);

}
