package com.quanlydetai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {
    private String userCode;
    private String fullName;
    private String email;
    private String phone;
    private Long roleId;
    private Long departmentId;
    private String academicRank;
    private String className;
    private String password;
}
