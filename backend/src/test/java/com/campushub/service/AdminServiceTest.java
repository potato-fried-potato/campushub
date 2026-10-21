package com.campushub.service;

import com.campushub.entity.Item;
import com.campushub.entity.Report;
import com.campushub.entity.User;
import com.campushub.mapper.ItemMapper;
import com.campushub.mapper.ReportMapper;
import com.campushub.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M5 管理后台：用户、商品与举报处置")
class AdminServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private ItemMapper itemMapper;
    @Mock private ReportMapper reportMapper;
    @InjectMocks private AdminService adminService;

    private User user(String role, String status) {
        User u = new User();
        u.setId(3L);
        u.setRole(role);
        u.setStatus(status);
        u.setPassword("$2a$hash");
        return u;
    }

    private Report report() {
        Report r = new Report();
        r.setId(7L);
        r.setItemId(10L);
        return r;
    }

    @Test
    @DisplayName("用户状态只允许 ACTIVE / DISABLED，其余返回 4001")
    void updateUserStatusRejectsIllegalStatus() {
        assertThat(adminService.updateUserStatus(3L, "BANNED")).isEqualTo(4001);
        verify(userMapper, never()).updateStatus(any(), anyString());
    }

    @Test
    @DisplayName("用户不存在时返回 4004")
    void updateUserStatusRejectsUnknownUser() {
        when(userMapper.getUserById(3L)).thenReturn(null);

        assertThat(adminService.updateUserStatus(3L, "DISABLED")).isEqualTo(4004);
    }

    @Test
    @DisplayName("管理员账号不能被禁用（4005）")
    void updateUserStatusRejectsAdminAccount() {
        when(userMapper.getUserById(3L)).thenReturn(user("ADMIN", "ACTIVE"));

        assertThat(adminService.updateUserStatus(3L, "DISABLED")).isEqualTo(4005);
        verify(userMapper, never()).updateStatus(any(), anyString());
    }

    @Test
    @DisplayName("禁用普通用户成功")
    void updateUserStatusDisablesUser() {
        when(userMapper.getUserById(3L)).thenReturn(user("USER", "ACTIVE"));

        assertThat(adminService.updateUserStatus(3L, "DISABLED")).isEqualTo(0);
        verify(userMapper).updateStatus(3L, "DISABLED");
    }

    @Test
    @DisplayName("用户列表不返回密码字段")
    void usersHidePassword() {
        when(userMapper.listForAdmin(null, null, 0, 10)).thenReturn(List.of(user("USER", "ACTIVE")));
        when(userMapper.countForAdmin(null, null)).thenReturn(1);

        Map<String, Object> data = adminService.users(null, null, 1, 10);

        @SuppressWarnings("unchecked")
        List<User> list = (List<User>) data.get("list");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPassword()).isNull();
    }

    @Test
    @DisplayName("强制下架不存在的商品返回 4004")
    void offShelfRejectsUnknownItem() {
        when(itemMapper.getItemById(10L)).thenReturn(null);

        assertThat(adminService.offShelf(10L)).isEqualTo(4004);
    }

    @Test
    @DisplayName("强制下架成功：商品状态置为 OFF_SHELF")
    void offShelfUpdatesStatus() {
        Item item = new Item();
        item.setId(10L);
        when(itemMapper.getItemById(10L)).thenReturn(item);

        assertThat(adminService.offShelf(10L)).isEqualTo(0);
        verify(itemMapper).updateStatus(10L, "OFF_SHELF");
    }

    @Test
    @DisplayName("举报不存在时处置返回 4004")
    void handleReportRejectsUnknownReport() {
        when(reportMapper.getById(7L)).thenReturn(null);

        assertThat(adminService.handleReport(7L, "OFF_SHELF", 1L)).isEqualTo(4004);
    }

    @Test
    @DisplayName("处置为下架：举报状态与商品状态同时更新")
    void handleReportOffShelvesItem() {
        when(reportMapper.getById(7L)).thenReturn(report());

        assertThat(adminService.handleReport(7L, "OFF_SHELF", 1L)).isEqualTo(0);

        verify(reportMapper).updateStatus(7L, "OFF_SHELF", 1L);
        verify(itemMapper).updateStatus(10L, "OFF_SHELF");
    }

    @Test
    @DisplayName("处置为驳回：只更新举报状态，商品不动")
    void handleReportRejectsReport() {
        when(reportMapper.getById(7L)).thenReturn(report());

        assertThat(adminService.handleReport(7L, "REJECT", 1L)).isEqualTo(0);

        verify(reportMapper).updateStatus(7L, "REJECTED", 1L);
        verify(itemMapper, never()).updateStatus(any(), anyString());
    }

    @Test
    @DisplayName("非法处置动作返回 4001")
    void handleReportRejectsIllegalAction() {
        when(reportMapper.getById(7L)).thenReturn(report());

        assertThat(adminService.handleReport(7L, "DELETE_EVERYTHING", 1L)).isEqualTo(4001);
        verify(reportMapper, never()).updateStatus(any(), anyString(), any());
    }
}
