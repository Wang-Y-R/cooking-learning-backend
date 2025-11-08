package com.example.cooking.dao.entity;

import lombok.Data;

@Data
public class TimeRequirement {
    private String duration;     // 10分钟/30秒
    private String type;         // exact / range / until_condition
}