package com.example.cooking.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recipeId;

    private Integer rating; // 1-5

    @Column(length = 2000)
    private String comment;

    private String imageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
}
