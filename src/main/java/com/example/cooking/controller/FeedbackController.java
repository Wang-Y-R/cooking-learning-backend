package com.example.cooking.controller;

import com.example.cooking.dao.entity.Feedback;
import com.example.cooking.dao.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;

    @PostMapping
    public ResponseEntity<Feedback> submit(@RequestBody Feedback feedback) {
        if (feedback.getRating() == null) {
            feedback.setRating(5);
        }
        return ResponseEntity.ok(feedbackRepository.save(feedback));
    }

    @GetMapping
    public ResponseEntity<List<Feedback>> list() {
        return ResponseEntity.ok(feedbackRepository.findAll());
    }
}
