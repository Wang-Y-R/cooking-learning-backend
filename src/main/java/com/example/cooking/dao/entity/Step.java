package com.example.cooking.dao.entity;

import lombok.Data;

@Data
public class Step {
    private Integer stepNumber;                  // 步骤号
    private String description;                  // 步骤说明
    private TimeRequirement timeRequirement;     // 时间要求（可为null）
    private String targetCondition;              // 目标状态
    private Boolean isBlockable;                 // 可否停顿做别的
    private String heatLevel;                    // 火候：大火/中火/小火/中大/中小/关火/null
}