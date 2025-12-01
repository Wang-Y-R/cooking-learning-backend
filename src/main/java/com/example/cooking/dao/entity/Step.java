package com.example.cooking.dao.entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "steps")
public class Step {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer stepNumber;                  // 步骤号

    @Column(length = 1000)
    private String description;                  // 步骤说明

    @Embedded
    private TimeRequirement timeRequirement;     // 时间要求（可为null）

    private String targetCondition;              // 目标状态

    private Boolean isBlockable;                 // 可否停顿做别的

    private String heatLevel;                    // 火候：大火/中火/小火/中大/中小/关火/null

    @Column(length = 500)
    private String note;                         // 备注

    @Transient
    private String imageUrl; // 不映射到数据库，用于返回给前端
}